// [전체 흐름에서의 위치] 브리핑 카드마다 하나씩 놓이는 스크랩(북마크) 버튼입니다.
// 자기만의 상태를 갖지 않는 순수 프레젠테이션 컴포넌트입니다 — 지금 스크랩된
// 상태인지(isScraped)와 클릭했을 때 무엇을 할지(onClick)를 모두 부모(BriefingPage)
// 로부터 받습니다. 빈 북마크/채워진 북마크를 짝으로 나타내는 표준 이모지가 없어서
// (⭐/📅처럼 이 프로젝트가 지금까지 쓰던 이모지 방식으로는 "빈 상태"를 표현할 수
// 없음), 작은 인라인 SVG 아이콘 하나로 stroke/fill만 바꿔 두 상태를 표현합니다 —
// 아이콘 라이브러리를 새로 추가하지 않기 위함입니다.
//
// [무엇을 받아서] isScraped(현재 스크랩 여부), onClick(클릭 시 호출할 콜백).
// [무엇을 하고] 북마크 모양 SVG를 그리되, isScraped면 색을 채우고 아니면 테두리만
//              그립니다. 버튼 자체의 클릭 이벤트를 그대로 onClick에 전달합니다.
// [무엇을 돌려주는지] 버튼 UI(JSX).
export default function ScrapButton({ isScraped, onClick }) {
  return (
    <button
      type="button"
      className="briefing-card__scrap"
      aria-label={isScraped ? "스크랩 취소" : "스크랩"}
      aria-pressed={isScraped}
      title={isScraped ? "스크랩 취소" : "스크랩"}
      onClick={onClick}
    >
      <svg
        width="18"
        height="18"
        viewBox="0 0 24 24"
        fill={isScraped ? "var(--accent)" : "none"}
        stroke="var(--accent)"
        strokeWidth="1.5"
        strokeLinejoin="round"
      >
        <path d="M5 3h14a1 1 0 0 1 1 1v17l-8-5-8 5V4a1 1 0 0 1 1-1z" />
      </svg>
    </button>
  );
}
