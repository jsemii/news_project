// [전체 흐름에서의 위치] 프론트엔드가 백엔드의 관리자 통계 API(GET /api/stats/*)를
// 호출하는 창구입니다. 이 4개 API는 전부 role이 ADMIN인 로그인 사용자만 호출할 수
// 있고, 그 외에는 403을 받습니다(백엔드 SecurityConfig가 강제) — 이 파일의 함수들은
// AdminStatsPage가 "이미 ADMIN으로 확인된 경우"에만 호출하므로 평소에는 403을 볼
// 일이 없지만, 혹시 그 사이 권한이 바뀌는 등의 이유로 403이 오더라도 에러로
// 던져서 호출한 쪽이 화면에 에러 문구를 보여줄 수 있게 합니다.

// [무엇을 받아서] 아무것도 받지 않습니다.
// [무엇을 하고] GET /api/stats/industries를 호출합니다.
// [무엇을 돌려주는지] 산업별 건수 목록({ industry, count }[]).
export async function fetchIndustryStats() {
  const response = await fetch("/api/stats/industries");
  if (!response.ok) {
    throw new Error(`산업별 통계를 불러오지 못했습니다. (status: ${response.status})`);
  }
  return response.json();
}

// [무엇을 받아서] 아무것도 받지 않습니다.
// [무엇을 하고] GET /api/stats/daily-collection을 호출합니다. 오늘 포함 최근
//              14일치를 항상 14개 항목으로 받습니다(수집 0건인 날짜도 포함).
// [무엇을 돌려주는지] 날짜별 수집 건수 목록({ date, count }[]).
export async function fetchDailyCollectionStats() {
  const response = await fetch("/api/stats/daily-collection");
  if (!response.ok) {
    throw new Error(`일별 수집 통계를 불러오지 못했습니다. (status: ${response.status})`);
  }
  return response.json();
}

// [무엇을 받아서] 아무것도 받지 않습니다.
// [무엇을 하고] GET /api/stats/job-scores를 호출합니다.
// [무엇을 돌려주는지] 직무별 평균 중요도 점수 목록({ job, avgScore }[]).
export async function fetchJobScoreStats() {
  const response = await fetch("/api/stats/job-scores");
  if (!response.ok) {
    throw new Error(`직무별 점수 통계를 불러오지 못했습니다. (status: ${response.status})`);
  }
  return response.json();
}

// [무엇을 받아서] 아무것도 받지 않습니다.
// [무엇을 하고] GET /api/stats/filtered를 호출합니다.
// [무엇을 돌려주는지] 필터링 사유별 건수 목록({ reason, count }[]).
export async function fetchFilteredStats() {
  const response = await fetch("/api/stats/filtered");
  if (!response.ok) {
    throw new Error(`필터링 통계를 불러오지 못했습니다. (status: ${response.status})`);
  }
  return response.json();
}
