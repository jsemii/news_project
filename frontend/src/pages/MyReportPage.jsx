import { useEffect, useState } from "react";
import { fetchMyScraps, fetchScrapIndustryStats } from "../api/scrapApi";
import { navigate } from "../hooks/useRoute";
import ScrapIndustryChart from "../components/ScrapIndustryChart";
import RecentScrapsList from "../components/RecentScrapsList";
import "./MyReportPage.css";

// [전체 흐름에서의 위치] 로그인한 사용자가 스크랩한 뉴스를 근거로 "어떤 산업에
// 관심을 가졌는지"를 보여주는 개인 리포트 화면입니다("/my-report"). AuthStatus의
// "내 리포트 보기" 링크로 들어오지만, 북마크/직접 접속으로 바로 들어올 수도
// 있으므로 마운트 시 독자적으로 데이터를 확인합니다.
//
// AdminStatsPage와 인증 확인 방식이 다릅니다 — AdminStatsPage는 role까지 봐야
// 해서 GET /api/auth/me를 따로 부르지만, 여기는 "로그인했는가"만 필요합니다.
// 이 페이지가 어차피 불러야 하는 GET /api/scraps·/api/scraps/industries 자체가
// 비로그인이면 403을 null로 변환해서 돌려주므로(scrapApi.js 참고), 그 결과
// 하나로 로그인 여부까지 같이 판단합니다 — /api/auth/me를 또 부를 필요가
// 없습니다. 접근 제어는 이중입니다: 여기(프론트)의 역할은 UX 편의일 뿐이고,
// 진짜 방어선은 백엔드 SecurityConfig의 "/api/scraps/**" 인증 필요 규칙입니다.
export default function MyReportPage() {
  const [loaded, setLoaded] = useState(false);
  const [scraps, setScraps] = useState(null);
  const [industryStats, setIndustryStats] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;

    Promise.all([fetchMyScraps(), fetchScrapIndustryStats()])
      .then(([scrapsResult, industryStatsResult]) => {
        if (!cancelled) {
          setScraps(scrapsResult);
          setIndustryStats(industryStatsResult);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err.message);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoaded(true);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  if (!loaded) {
    return <main className="my-report-page" />;
  }

  if (error) {
    return (
      <main className="my-report-page my-report-page--denied">
        <p>{error}</p>
      </main>
    );
  }

  // scraps가 null이면 fetchMyScraps()가 403을 받았다는 뜻(비로그인) — 이 값
  // 하나로 로그인 여부를 판단합니다(위 클래스 주석 참고).
  if (scraps === null) {
    return (
      <main className="my-report-page my-report-page--denied">
        <p>로그인이 필요합니다.</p>
      </main>
    );
  }

  return (
    <main className="my-report-page">
      <header className="my-report-page__header">
        <h1 className="my-report-page__title">내 리포트</h1>
        <a
          className="my-report-page__back"
          href="/"
          onClick={(e) => {
            e.preventDefault();
            navigate("/");
          }}
        >
          브리핑으로 돌아가기
        </a>
      </header>

      {scraps.length === 0 ? (
        <p className="my-report-page__empty">
          아직 스크랩한 뉴스가 없습니다. 브리핑에서 관심 있는 뉴스를 스크랩해보세요.
        </p>
      ) : (
        <>
          <div className="my-report-page__summary">
            <div className="my-report-page__summary-card">
              <span className="my-report-page__summary-value">{scraps.length}</span>
              <span className="my-report-page__summary-label">총 스크랩</span>
            </div>
            <div className="my-report-page__summary-card">
              <span className="my-report-page__summary-value">{industryStats.length}</span>
              <span className="my-report-page__summary-label">관심 산업 수</span>
            </div>
            <div className="my-report-page__summary-card">
              <span className="my-report-page__summary-value">{industryStats[0]?.industry ?? "-"}</span>
              <span className="my-report-page__summary-label">최다 관심 산업</span>
            </div>
          </div>

          {industryStats.length > 0 && (
            <section className="my-report-page__section">
              <h2 className="my-report-page__section-title">산업 관심도</h2>
              <div className="my-report-page__chart">
                <ScrapIndustryChart data={industryStats} />
              </div>
            </section>
          )}

          <section className="my-report-page__section">
            <h2 className="my-report-page__section-title">최근 스크랩</h2>
            <RecentScrapsList items={scraps} />
          </section>
        </>
      )}
    </main>
  );
}
