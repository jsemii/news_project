import { useEffect, useState } from "react";
import { fetchBriefings } from "../api/briefingApi";
import { addScrap, fetchMyScraps, removeScrap } from "../api/scrapApi";
import JobSelector from "../components/JobSelector";
import DateNavigator from "../components/DateNavigator";
import DailyHighlight from "../components/DailyHighlight";
import AuthStatus from "../components/AuthStatus";
import LoginModal from "../components/LoginModal";
import ScrapButton from "../components/ScrapButton";
import { getTodayString } from "../utils/dateUtils";
import "./BriefingPage.css";

// [무엇을 받아서] ISO 형식 날짜/시간 문자열(예: "2026-08-18T16:30:00").
// [무엇을 하고] 화면에 표시하기 좋은 "2026.08.18" 형태로 바꿉니다.
// [무엇을 돌려주는지] 변환된 날짜 문자열. 값이 없으면 빈 문자열.
function formatDate(publishedAt) {
  if (!publishedAt) return "";
  const date = new Date(publishedAt);
  return date.toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });
}

// [전체 흐름에서의 위치] "직무별 맞춤 브리핑"(핵심기능3) 화면 전체를 그리는 페이지
// 컴포넌트입니다. JobSelector(직무 선택 탭)와 briefingApi(서버 호출)를 조합해서,
// 사용자가 직무를 고르면 그 직무 관점으로 재해석된 브리핑을, 고르지 않으면(전체) 기존
// 공통 요약 브리핑을 카드 목록으로 보여줍니다.
export default function BriefingPage() {
  // selectedJob이 null이면 "전체"(일반 모드), 아니면 선택된 직무 이름 문자열입니다.
  const [selectedJob, setSelectedJob] = useState(null);
  // selectedDate는 항상 "yyyy-MM-dd" 문자열입니다. 기본값은 오늘.
  const [selectedDate, setSelectedDate] = useState(getTodayString());
  const [briefings, setBriefings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  // scrapedNewsIds가 null이면 "아직 확인 전 또는 비로그인"을 뜻합니다(로딩 중에는
  // 카드에 스크랩 버튼이 항상 빈 상태로 보이다가, 확인되면 실제 상태로 바뀝니다).
  // fetchMyScraps()가 403(비로그인)을 null로 변환해주므로, 이 값 하나로 로그인
  // 여부와 스크랩 목록을 동시에 판단합니다 — /api/auth/me를 따로 호출할 필요가 없습니다.
  const [scrapedNewsIds, setScrapedNewsIds] = useState(null);
  const [isLoginModalOpen, setLoginModalOpen] = useState(false);

  // 마운트 시 한 번만 내 스크랩 목록을 불러옵니다. 로그인은 OAuth2 리다이렉트
  // 왕복(실제 페이지 이동)으로만 이뤄지므로, 로그인 성공 시 앱이 통째로 다시
  // 로드되면서 이 useEffect도 다시 실행됩니다 — SPA 안에서 로그인 상태 변화를
  // 별도로 감지할 필요가 없습니다(AuthStatus가 마운트 시 한 번만 확인하는 것과
  // 같은 이유). "내가 관심 가진 산업" 위젯은 별도 페이지(/my-report)로 옮겨져서
  // 여기서는 더 이상 산업별 통계를 불러오지 않습니다 — 카드의 북마크 아이콘
  // 상태 판단에 필요한 newsId 목록만 있으면 됩니다.
  useEffect(() => {
    let cancelled = false;

    fetchMyScraps()
      .then((scraps) => {
        if (!cancelled) {
          setScrapedNewsIds(scraps === null ? null : new Set(scraps.map((s) => s.newsId)));
        }
      })
      .catch(() => {
        // 조용히 무시합니다: 스크랩 상태 확인 실패가 브리핑 화면 자체를 막을
        // 이유는 없습니다(스크랩 버튼이 전부 빈 상태로 보이는 정도로 충분).
      });

    return () => {
      cancelled = true;
    };
  }, []);

  // [무엇을 받아서] 스크랩 버튼을 누른 뉴스의 id.
  // [무엇을 하고] scrapedNewsIds가 null이면(비로그인) 로그인 모달을 엽니다. 아니면
  //              현재 스크랩 여부에 따라 추가/취소 API를 호출하고, 응답이 온 뒤에
  //              (낙관적 업데이트 없이 — AuthStatus의 로그아웃과 같은 관례) 로컬
  //              Set을 갱신합니다.
  // [무엇을 돌려주는지] 없음.
  function handleToggleScrap(newsId) {
    if (scrapedNewsIds === null) {
      setLoginModalOpen(true);
      return;
    }

    if (scrapedNewsIds.has(newsId)) {
      removeScrap(newsId)
        .then(() => {
          setScrapedNewsIds((prev) => {
            const next = new Set(prev);
            next.delete(newsId);
            return next;
          });
        })
        .catch(() => {
          // 실패해도 화면을 막지 않습니다 — 버튼 상태가 그대로 유지되어 다시
          // 시도할 수 있습니다.
        });
    } else {
      addScrap(newsId)
        .then(() => {
          setScrapedNewsIds((prev) => new Set(prev).add(newsId));
        })
        .catch(() => {
          // 위와 같은 이유로 조용히 무시합니다.
        });
    }
  }

  // selectedJob이나 selectedDate가 바뀔 때마다(탭을 누르거나 날짜를 이동/선택할 때마다)
  // 서버에서 다시 목록을 받아옵니다. 둘은 서로 독립적인 state라서, 예를 들어 "IT전산" +
  // "8월 15일"처럼 두 조건을 동시에 적용해 조회할 수 있습니다. 백엔드가 이미
  // importance_score 내림차순으로 정렬해서 응답을 주므로, 프론트에서 다시 정렬할 필요는 없습니다.
  useEffect(() => {
    let cancelled = false;

    setLoading(true);
    setError(null);
    // 날짜/직무를 바꾸는 순간 이전 선택의 카드 목록을 바로 비웁니다. 이게 없으면, 예를
    // 들어 데이터가 없는 날짜로 이동했을 때 요청이 실패하거나 늦게 응답하는 경우
    // 직전 날짜의 카드가 화면에 그대로 남아있는 것처럼 보일 수 있습니다(실제로 겪은
    // 문제 — docs/troubleshooting.md 참고). 새 응답이 오기 전까지는 "불러오는 중"
    // 상태만 보이는 게 맞습니다.
    setBriefings([]);

    fetchBriefings(selectedJob, selectedDate)
      .then((data) => {
        if (!cancelled) {
          setBriefings(data);
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

    // cleanup: 사용자가 응답이 오기 전에 탭/날짜를 빠르게 여러 번 바꾸면, 먼저 보낸
    // 요청의 응답이 나중에 도착해서 최신 선택을 덮어쓸 수 있습니다(경쟁 상태).
    // cancelled 플래그로 "이미 지나간 요청"의 결과는 화면에 반영하지 않도록 막습니다.
    return () => {
      cancelled = true;
    };
  }, [selectedJob, selectedDate]);

  const isToday = selectedDate === getTodayString();
  const [, month, day] = selectedDate.split("-").map(Number);
  const title = isToday ? "오늘의 브리핑" : `${month}월 ${day}일 브리핑`;

  return (
    <main className="briefing-page">
      <header className="briefing-page__header">
        <AuthStatus />
        <h1 className="briefing-page__title">{title}</h1>
        <p className="briefing-page__subtitle">
          관심 직무를 선택하면 그 직무 관점에서 뉴스를 다시 읽어드립니다.
        </p>
        <DailyHighlight date={selectedDate} />

        <div className="briefing-page__controls">
          <JobSelector selectedJob={selectedJob} onSelect={setSelectedJob} />
          <DateNavigator selectedDate={selectedDate} onChange={setSelectedDate} />
        </div>
      </header>

      {loading && <p className="briefing-page__status">불러오는 중...</p>}
      {error && <p className="briefing-page__status briefing-page__status--error">{error}</p>}
      {!loading && !error && briefings.length === 0 && (
        <p className="briefing-page__status">해당 날짜엔 브리핑이 없습니다.</p>
      )}

      <ul className="briefing-list">
        {briefings.map((item) => (
          <BriefingCard
            key={item.newsId}
            item={item}
            isScraped={scrapedNewsIds?.has(item.newsId) ?? false}
            onToggleScrap={() => handleToggleScrap(item.newsId)}
          />
        ))}
      </ul>

      <LoginModal
        open={isLoginModalOpen}
        onClose={() => setLoginModalOpen(false)}
        message="로그인 후 이용하십시오."
      />
    </main>
  );
}

// [무엇을 받아서] 브리핑 항목 하나(BriefingItem 응답 그대로: title/url/summary/
//              importanceScore/industries/jobInsight), 이 뉴스가 지금 스크랩돼
//              있는지(isScraped), 스크랩 버튼을 눌렀을 때 호출할 콜백(onToggleScrap —
//              로그인 여부 판단과 실제 API 호출은 전부 부모(BriefingPage)가 담당하고,
//              이 컴포넌트는 그 결과만 받아서 보여줍니다).
// [무엇을 하고] 항상 보이는 공통 정보(제목/날짜/중요도/산업 태그/공통 요약)를 카드로
//              그리고, jobInsight가 있으면(직무를 선택한 경우) "왜 중요한가"/"핵심 역량"
//              섹션을 라벨로 구분해서 추가로 보여줍니다. 스크랩 버튼은 산업 배지가
//              있는 줄(.briefing-card__meta)의 맨 마지막(날짜 다음)에 둡니다 —
//              그 줄은 이미 날짜가 margin-left:auto로 오른쪽 끝에 붙어있어서, 버튼을
//              날짜 뒤에 추가하는 것만으로 CSS 변경 없이 줄의 오른쪽 끝에 위치하고,
//              제목 줄에 따로 있는 ⭐(isJobHighlighted)와도 겹치지 않습니다.
// [무엇을 돌려주는지] 카드 하나(li 엘리먼트).
function BriefingCard({ item, isScraped, onToggleScrap }) {
  const keySkillTags = item.jobInsight?.keySkills
    ? item.jobInsight.keySkills.split(",").map((skill) => skill.trim()).filter(Boolean)
    : [];

  return (
    <li className="briefing-card">
      <div className="briefing-card__meta">
        <span className="briefing-card__score">중요도 {item.importanceScore}</span>
        {item.industries.map((industry) => (
          <span key={industry} className="briefing-card__tag">
            {industry}
          </span>
        ))}
        <span className="briefing-card__date">{formatDate(item.publishedAt)}</span>
        <ScrapButton isScraped={isScraped} onClick={onToggleScrap} />
      </div>

      <a className="briefing-card__title" href={item.url}>
        {item.isJobHighlighted && (
          <span className="briefing-card__job-star" title="이 직무와 강하게 관련된 뉴스">
            ⭐
          </span>
        )}
        {item.title}
      </a>

      <p className="briefing-card__summary">{item.summary}</p>

      {item.jobInsight && (
        <div className="briefing-card__insight">
          <div className="briefing-card__insight-row">
            <span className="briefing-card__insight-label briefing-card__insight-label--why">
              왜 중요한가
            </span>
            <p className="briefing-card__insight-text">{item.jobInsight.whyItMatters}</p>
          </div>
          <div className="briefing-card__insight-row">
            <span className="briefing-card__insight-label briefing-card__insight-label--skills">
              핵심 역량
            </span>
            <div className="briefing-card__skill-tags">
              {keySkillTags.map((skill) => (
                <span key={skill} className="briefing-card__skill-tag">
                  {skill}
                </span>
              ))}
            </div>
          </div>
        </div>
      )}
    </li>
  );
}
