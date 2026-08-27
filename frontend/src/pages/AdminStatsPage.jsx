import { useEffect, useState } from "react";
import { fetchCurrentUser } from "../api/authApi";
import {
  fetchDailyCollectionStats,
  fetchFilteredStats,
  fetchIndustryStats,
  fetchJobScoreStats,
  fetchSignupStats,
} from "../api/statsApi";
import { navigate } from "../hooks/useRoute";
import StatCard from "../components/StatCard";
import IndustryPieChart from "../components/IndustryPieChart";
import DailyCollectionChart from "../components/DailyCollectionChart";
import JobScoreChart from "../components/JobScoreChart";
import FilteredReasonChart from "../components/FilteredReasonChart";
import SignupChart from "../components/SignupChart";
import "./AdminStatsPage.css";

// [전체 흐름에서의 위치] 관리자 전용 통계 대시보드 화면입니다. "/admin" 경로로
// 들어오면(북마크/공유 링크로 다른 화면을 거치지 않고 바로 들어올 수도 있으므로)
// 이 페이지가 마운트되자마자 독자적으로 GET /api/auth/me를 호출해서 로그인
// 상태와 role을 직접 확인합니다 — AuthStatus가 이미 확인해둔 값을 물려받지
// 않습니다.
//
// 접근 제어는 이중으로 걸려 있습니다. 여기(프론트)의 역할은 어디까지나 UX
// 편의입니다 — role이 ADMIN으로 확인되기 전에는 차트를 그리는 컴포넌트 자체가
// 아예 마운트되지 않으므로(아래 조건부 렌더링 참고) /api/stats/* 요청이 처음부터
// 나가지 않습니다. 진짜 보안 경계는 백엔드 SecurityConfig의 hasRole("ADMIN")
// 규칙입니다 — 프론트를 우회해서 /api/stats/*를 직접 호출해도 ADMIN이 아니면
// 403을 받습니다.
export default function AdminStatsPage() {
  const [authChecked, setAuthChecked] = useState(false);
  const [user, setUser] = useState(null);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;

    fetchCurrentUser()
      .then((data) => {
        if (!cancelled) {
          setUser(data);
        }
      })
      .catch(() => {
        // AuthStatus와 같은 이유로 조용히 무시합니다: 실패하면 비로그인으로
        // 취급하는 것으로 충분합니다.
      })
      .finally(() => {
        if (!cancelled) {
          setAuthChecked(true);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const isAdmin = user?.role === "ADMIN";

  useEffect(() => {
    // authChecked가 아니거나 ADMIN이 아니면 여기서 그냥 끝냅니다 — 이 useEffect가
    // /api/stats/* 요청 4개를 보내는 유일한 지점이므로, 이 한 줄이 곧 "관리자가
    // 아니면 통계 데이터를 애초에 요청하지 않는다"는 프론트 쪽 접근 제어의 실체입니다.
    if (!authChecked || !isAdmin) {
      return;
    }

    let cancelled = false;
    setLoading(true);
    setError(null);

    Promise.all([
      fetchIndustryStats(),
      fetchDailyCollectionStats(),
      fetchJobScoreStats(),
      fetchFilteredStats(),
      fetchSignupStats(),
    ])
      .then(([industries, dailyCollection, jobScores, filtered, signups]) => {
        if (!cancelled) {
          setStats({ industries, dailyCollection, jobScores, filtered, signups });
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err.message);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [authChecked, isAdmin]);

  if (!authChecked) {
    return <main className="admin-stats-page" />;
  }

  if (!isAdmin) {
    return (
      <main className="admin-stats-page admin-stats-page--denied">
        <p>관리자만 접근 가능합니다.</p>
      </main>
    );
  }

  return (
    <main className="admin-stats-page">
      <header className="admin-stats-page__header">
        <h1 className="admin-stats-page__title">관리자 통계</h1>
        <a
          className="admin-stats-page__back"
          href="/"
          onClick={(e) => {
            e.preventDefault();
            navigate("/");
          }}
        >
          브리핑으로 돌아가기
        </a>
      </header>

      {loading && <p className="admin-stats-page__status">불러오는 중...</p>}
      {error && <p className="admin-stats-page__status admin-stats-page__status--error">{error}</p>}

      {stats && (
        <div className="admin-stats-page__grid">
          <StatCard title="산업별 뉴스 건수">
            <IndustryPieChart data={stats.industries} />
          </StatCard>
          <StatCard title="최근 14일 일별 수집 건수">
            <DailyCollectionChart data={stats.dailyCollection} />
          </StatCard>
          <StatCard
            title="직무별 평균 중요도 점수"
            subtitle="실제로 점수가 매겨진 뉴스만 평균 냈어요 (점수가 없는 예전 뉴스는 제외)"
          >
            <JobScoreChart data={stats.jobScores} />
          </StatCard>
          <StatCard
            title="필터링 사유별 건수"
            subtitle="TOO_OLD(오래된 뉴스)·CONTENT_TOO_SHORT(본문이 짧은 뉴스)·TITLE_EXCLUDED(제목에 제외 키워드 포함)는 AI가 보기 전에 미리 걸러진 뉴스, UNKNOWN은 이 구분이 생기기 전의 예전 뉴스예요"
          >
            <FilteredReasonChart data={stats.filtered} />
          </StatCard>
          <StatCard title="최근 14일 일별 회원가입 추이">
            <SignupChart data={stats.signups} />
          </StatCard>
        </div>
      )}
    </main>
  );
}
