// [전체 흐름에서의 위치] 브리핑 날짜 탐색 기능(BriefingPage, DateNavigator)이 공통으로
// 쓰는 순수 날짜 계산 함수 모음입니다. 여기엔 화면을 그리는 코드가 전혀 없고, "yyyy-MM-dd"
// 문자열을 계산해서 돌려주는 역할만 합니다 — 백엔드 GET /api/briefings의 date 파라미터가
// 정확히 이 형식(yyyy-MM-dd)을 기대하기 때문에, 프론트 어디서든 날짜를 이 형식의 문자열로
// 다룹니다.

// [무엇을 받아서] 연/월/일(Date 객체에서 뽑은 로컬 타임존 기준 값).
// [무엇을 하고] "yyyy-MM-dd" 형식으로 0을 채워 두 자리로 맞춥니다.
// [무엇을 돌려주는지] "2026-08-05"처럼 항상 10글자인 날짜 문자열.
function toDateString(year, month, day) {
  const mm = String(month).padStart(2, "0");
  const dd = String(day).padStart(2, "0");
  return `${year}-${mm}-${dd}`;
}

// [무엇을 받아서] 입력값 없음.
// [무엇을 하고] 브라우저의 "로컬" 타임존 기준으로 오늘 날짜를 구합니다.
//              Date.toISOString()을 안 쓰는 이유: 그건 UTC 기준이라, 한국 시간
//              자정 근처(예: 새벽 0~9시)에는 UTC로 아직 "어제"라서 날짜가 하루
//              밀리는 버그가 날 수 있습니다. getFullYear/getMonth/getDate는 항상
//              브라우저가 있는 그 지역의 로컬 날짜를 돌려주므로 이 문제가 없습니다.
// [무엇을 돌려주는지] 오늘 날짜의 "yyyy-MM-dd" 문자열.
export function getTodayString() {
  const now = new Date();
  return toDateString(now.getFullYear(), now.getMonth() + 1, now.getDate());
}

// [무엇을 받아서] 기준이 되는 "yyyy-MM-dd" 문자열과, 며칠을 이동할지(deltaDays,
//              음수면 과거로, 양수면 미래로).
// [무엇을 하고] 문자열을 Date 객체로 바꾼 뒤 날짜만 이동시키고, 다시 "yyyy-MM-dd"로 바꿉니다.
// [무엇을 돌려주는지] 이동한 날짜의 "yyyy-MM-dd" 문자열.
export function shiftDateString(dateString, deltaDays) {
  const [year, month, day] = dateString.split("-").map(Number);
  // new Date(year, monthIndex, day)는 로컬 타임존 기준으로 만들어지므로, UTC 변환 없이
  // day에 deltaDays를 더하면 자바스크립트가 월/연도 경계까지 알아서 계산해줍니다
  // (예: 8월 31일 + 1 = 9월 1일).
  const date = new Date(year, month - 1, day + deltaDays);
  return toDateString(date.getFullYear(), date.getMonth() + 1, date.getDate());
}
