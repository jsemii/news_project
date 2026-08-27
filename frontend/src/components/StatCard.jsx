import "./StatCard.css";

// [전체 흐름에서의 위치] 관리자 통계 대시보드(AdminStatsPage)의 차트 4개가 공유하는
// 카드 뼈대입니다. 제목과 차트 콘텐츠(children)만 받아서 테두리/여백/제목 스타일을
// 통일합니다 — 차트별로 이 뼈대를 각자 다시 그리지 않기 위한 공통 컴포넌트입니다.
//
// [무엇을 받아서] title(카드 제목 문자열), subtitle(선택, 집계 기준 같은 보충
//              설명 — 없으면 렌더링하지 않음), children(내부에 그릴 실제 차트).
// [무엇을 하고] 제목(+있으면 부제) 아래에 children을 그대로 렌더링합니다.
//              차트 로직 자체는 전혀 모릅니다(순수 레이아웃 담당).
// [무엇을 돌려주는지] 카드 UI(JSX).
export default function StatCard({ title, subtitle, children }) {
  return (
    <div className="stat-card">
      <div className="stat-card__header">
        <h3 className="stat-card__title">{title}</h3>
        {subtitle && <p className="stat-card__subtitle">{subtitle}</p>}
      </div>
      <div className="stat-card__body">{children}</div>
    </div>
  );
}
