// [전체 흐름에서의 위치] 프론트엔드가 백엔드의 스크랩(북마크) API(/api/scraps)를
// 호출하는 창구입니다. 이 API들은 전부 로그인이 필요합니다 — 백엔드
// SecurityConfig가 "/api/scraps/**"를 인증 필요로 막아두고, 비로그인 요청은
// 컨트롤러에 도달하지도 못한 채 공통 entry point가 403을 돌려줍니다.
//
// 주의: fetchCurrentUser()(authApi.js)는 "비로그인=401"이지만, 여기 fetchMyScraps()는
// "비로그인=403"입니다 — 겉보기엔 같은 패턴(비로그인 → null)처럼 보여도 실제 상태
// 코드가 다릅니다. /api/auth/me는 permitAll이라 컨트롤러가 직접 401을 만들어 응답하는
// 반면, /api/scraps/**는 Spring Security가 인증 필요로 막아서 컨트롤러 밖에서 403이
// 만들어지기 때문입니다(SecurityConfig 주석 참고). 두 함수를 비슷하게 베껴 쓸 때
// 상태 코드까지 그대로 복사하면 안 됩니다.

// [무엇을 받아서] 아무것도 받지 않습니다.
// [무엇을 하고] GET /api/scraps를 호출합니다. 403(비로그인)이면 예외를 던지지
//              않고 조용히 null을 돌려줍니다(fetchCurrentUser의 401→null과 같은
//              철학 — 비로그인은 에러가 아니라 정상 상태).
// [무엇을 돌려주는지] 로그인 상태면 스크랩 목록({ id, newsId, createdAt }[]), 아니면 null.
export async function fetchMyScraps() {
  const response = await fetch("/api/scraps");

  if (response.status === 403) {
    return null;
  }
  if (!response.ok) {
    throw new Error(`스크랩 목록을 불러오지 못했습니다. (status: ${response.status})`);
  }

  return response.json();
}

// [무엇을 받아서] 스크랩할 뉴스 id(newsId).
// [무엇을 하고] POST /api/scraps?newsId=...를 호출합니다. 이미 스크랩한 뉴스를
//              다시 요청해도 서버가 에러 없이 기존 스크랩을 그대로 돌려줍니다.
// [무엇을 돌려주는지] 생성(또는 기존) 스크랩 항목({ id, newsId, createdAt }).
export async function addScrap(newsId) {
  const response = await fetch(`/api/scraps?newsId=${newsId}`, { method: "POST" });

  if (!response.ok) {
    throw new Error(`스크랩에 실패했습니다. (status: ${response.status})`);
  }

  return response.json();
}

// [무엇을 받아서] 스크랩을 취소할 뉴스 id(newsId).
// [무엇을 하고] DELETE /api/scraps/{newsId}를 호출합니다. 스크랩한 적이 없어도
//              서버가 에러 없이 200을 돌려줍니다(멱등).
// [무엇을 돌려주는지] 없음. 실패하면 예외를 던집니다.
export async function removeScrap(newsId) {
  const response = await fetch(`/api/scraps/${newsId}`, { method: "DELETE" });

  if (!response.ok) {
    throw new Error(`스크랩 취소에 실패했습니다. (status: ${response.status})`);
  }
}

// [무엇을 받아서] 아무것도 받지 않습니다.
// [무엇을 하고] GET /api/scraps/industries를 호출합니다. 403(비로그인)이면
//              fetchMyScraps와 같은 이유로 조용히 null을 돌려줍니다.
// [무엇을 돌려주는지] 로그인 상태면 산업별 건수 목록({ industry, count }[]), 아니면 null.
export async function fetchScrapIndustryStats() {
  const response = await fetch("/api/scraps/industries");

  if (response.status === 403) {
    return null;
  }
  if (!response.ok) {
    throw new Error(`관심 산업 통계를 불러오지 못했습니다. (status: ${response.status})`);
  }

  return response.json();
}
