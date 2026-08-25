// [전체 흐름에서의 위치] 프론트엔드가 백엔드의 "직무별 맞춤 브리핑" 조회 API
// (GET /api/briefings)를 호출하는 창구입니다. UI 로직은 전혀 없고, "서버에 요청해서
// 데이터를 받아온다"는 역할만 합니다(BriefingPage가 이 함수를 불러서 씁니다).

// [무엇을 받아서] job(직무 이름 문자열, 예: "IT전산") 또는 null/undefined(직무를
//              선택하지 않은 "일반 모드"), date("yyyy-MM-dd" 날짜 문자열, 보통
//              dateUtils.getTodayString()이나 그걸 이동시킨 값).
// [무엇을 하고] job이 있으면 job도 쿼리에 붙이고, date는 항상 붙입니다(백엔드도 date가
//              없으면 오늘로 처리하지만, 프론트는 selectedDate state를 항상 갖고 있으므로
//              매번 명시적으로 보내는 편이 더 단순합니다). 예: "/api/briefings?job=IT전산&date=2026-08-15".
//              요청은 vite.config.js의 프록시 설정을 통해 실제로는 http://localhost:8080으로
//              전달됩니다(CORS 회피).
// [무엇을 돌려주는지] 성공하면 브리핑 목록(JSON 배열, 그 날짜에 데이터가 없으면 빈 배열)을
//              그대로 돌려줍니다. 실패하면(예: 존재하지 않는 job 값이거나 날짜 형식이 잘못돼서
//              400이 온 경우) 에러를 던져서, 호출한 쪽(BriefingPage)이 catch해서 화면에
//              에러 메시지를 보여줄 수 있게 합니다.
export async function fetchBriefings(job, date) {
  const params = new URLSearchParams();
  if (job) params.set("job", job);
  if (date) params.set("date", date);

  const response = await fetch(`/api/briefings?${params.toString()}`);

  if (!response.ok) {
    throw new Error(`브리핑을 불러오지 못했습니다. (status: ${response.status})`);
  }

  return response.json();
}

// [무엇을 받아서] date("yyyy-MM-dd" 날짜 문자열).
// [무엇을 하고] GET /api/briefings/highlight를 호출합니다. 백엔드는 그날 "오늘 한 줄
//              요약"이 아직 없으면(재료 뉴스가 부족했거나 배치가 안 돈 경우) 204 No
//              Content로 응답합니다 — 이건 에러가 아니라 정상적인 "없음" 상태라서,
//              이 함수는 이 경우 조용히 null을 돌려줍니다(예외를 던지지 않음).
// [무엇을 돌려주는지] 요약이 있으면 { date, headline, basedOnCount } 객체, 없으면 null.
//              그 외 실패(네트워크 오류 등)는 예외를 던져서 호출한 쪽이 처리하게 합니다.
export async function fetchDailyHighlight(date) {
  const params = new URLSearchParams();
  if (date) params.set("date", date);

  const response = await fetch(`/api/briefings/highlight?${params.toString()}`);

  if (response.status === 204) {
    return null;
  }
  if (!response.ok) {
    throw new Error(`오늘의 요약을 불러오지 못했습니다. (status: ${response.status})`);
  }

  return response.json();
}
