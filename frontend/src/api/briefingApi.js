// [전체 흐름에서의 위치] 프론트엔드가 백엔드의 "직무별 맞춤 브리핑" 조회 API
// (GET /api/briefings)를 호출하는 창구입니다. UI 로직은 전혀 없고, "서버에 요청해서
// 데이터를 받아온다"는 역할만 합니다(BriefingPage가 이 함수를 불러서 씁니다).

// [무엇을 받아서] job(직무 이름 문자열, 예: "IT전산") 또는 null/undefined(직무를
//              선택하지 않은 "일반 모드").
// [무엇을 하고] job이 있으면 "/api/briefings?job=IT전산"처럼 쿼리 파라미터를 붙이고,
//              없으면 파라미터 없이 "/api/briefings"만 호출합니다. 요청은 vite.config.js의
//              프록시 설정을 통해 실제로는 http://localhost:8080으로 전달됩니다(CORS 회피).
// [무엇을 돌려주는지] 성공하면 브리핑 목록(JSON 배열)을 그대로 돌려줍니다. 실패하면
//              (예: 존재하지 않는 job 값이라 400이 온 경우) 에러를 던져서, 호출한 쪽
//              (BriefingPage)이 catch해서 화면에 에러 메시지를 보여줄 수 있게 합니다.
export async function fetchBriefings(job) {
  const query = job ? `?job=${encodeURIComponent(job)}` : "";
  const response = await fetch(`/api/briefings${query}`);

  if (!response.ok) {
    throw new Error(`브리핑을 불러오지 못했습니다. (status: ${response.status})`);
  }

  return response.json();
}
