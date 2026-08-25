import { useEffect, useState } from "react";
import { fetchDailyHighlight } from "../api/briefingApi";
import "./DailyHighlight.css";

// [전체 흐름에서의 위치] BriefingPage 상단(JobSelector 위쪽)에 놓이는 "오늘 한 줄 요약"
// 배너입니다. 직무 필터와 무관하게(백엔드가 직무 필터 없는 전체 뉴스로 계산하므로)
// 오직 selectedDate에만 반응합니다. 카드 목록처럼 "불러오는 중"/에러 문구를 따로
// 보여주지 않고 조용히 있다가 나타나는 이유: 이 배너는 브리핑 화면의 핵심 콘텐츠가
// 아니라 보조 정보라서, 로딩 상태를 드러내면 오히려 화면이 산만해집니다. 요약이 없는
// 날짜(재료 뉴스가 부족했거나 아직 배치가 안 돈 경우)는 그냥 아무것도 렌더링하지
// 않습니다 — 에러가 아니라 정상 상태이기 때문입니다.
export default function DailyHighlight({ date }) {
  const [highlight, setHighlight] = useState(null);

  useEffect(() => {
    let cancelled = false;

    // 날짜가 바뀌는 순간 이전 날짜의 요약을 바로 지웁니다. BriefingPage의 카드 목록과
    // 같은 이유입니다 — 새 응답이 오기 전까지 직전 날짜의 배너가 잘못 남아있으면 안 됩니다.
    setHighlight(null);

    fetchDailyHighlight(date)
      .then((data) => {
        if (!cancelled) {
          setHighlight(data);
        }
      })
      .catch(() => {
        // 조용히 무시합니다: 이 배너는 보조 정보라서, 실패해도 화면에 에러를 노출하지
        // 않고 그냥 안 보이는 상태로 둡니다(카드 목록은 핵심 콘텐츠라 에러를 보여주지만,
        // 이 배너는 그렇지 않습니다).
      });

    return () => {
      cancelled = true;
    };
  }, [date]);

  if (!highlight) {
    return null;
  }

  return (
    <div className="daily-highlight">
      <span className="daily-highlight__label">오늘의 흐름</span>
      <p className="daily-highlight__text">{highlight.headline}</p>
    </div>
  );
}
