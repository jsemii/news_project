// [무엇을 받아서] 스크랩 목록(items: GET /api/scraps 응답 그대로 — id/newsId/
//              title/url/publishedAt/createdAt/industries[]).
// [무엇을 하고] 최신 스크랩순(서버가 이미 정렬해서 줌)으로 제목·발행일·산업
//              배지를 한 줄씩 보여줍니다. 제목은 원문 링크입니다(BriefingPage의
//              카드 제목과 같은 방식).
// [무엇을 돌려주는지] 목록 UI(JSX). items가 비어있으면 안내 문구를 보여줍니다
//              (스크랩이 아직 없는 것은 에러가 아니라 정상 상태 — 이 프로젝트
//              전반의 "데이터 없음" 처리 관례).
export default function RecentScrapsList({ items }) {
  if (items.length === 0) {
    return <p className="my-report-page__empty-list">아직 스크랩한 뉴스가 없습니다.</p>;
  }

  return (
    <ul className="my-report-page__scrap-list">
      {items.map((item) => (
        <li key={item.id} className="my-report-page__scrap-row">
          <a className="my-report-page__scrap-title" href={item.url}>
            {item.title}
          </a>
          <div className="my-report-page__scrap-meta">
            {item.industries.map((industry) => (
              <span key={industry} className="my-report-page__scrap-tag">
                {industry}
              </span>
            ))}
            <span className="my-report-page__scrap-date">
              {item.publishedAt ? item.publishedAt.slice(0, 10) : ""}
            </span>
          </div>
        </li>
      ))}
    </ul>
  );
}
