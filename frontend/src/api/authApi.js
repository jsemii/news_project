// [전체 흐름에서의 위치] 프론트엔드가 백엔드의 로그인 상태 확인/로그아웃 API를
// 호출하는 창구입니다. GitHub/Google 로그인 자체는 fetch가 아니라 <a href="/oauth2/
// authorization/{provider}">로 브라우저를 직접 이동시키는 방식이라 여기 없습니다
// (AuthStatus.jsx 참고) — 여기서는 "지금 로그인되어 있는지"와 "로그아웃"만 다룹니다.

// [무엇을 받아서] 아무것도 받지 않습니다.
// [무엇을 하고] GET /api/auth/me를 호출합니다. 로그인 상태가 아니면 백엔드가 401을
//              돌려주는데, 이건 에러가 아니라 "비로그인"이라는 정상 상태이므로 예외를
//              던지지 않고 조용히 null을 돌려줍니다(fetchDailyHighlight가 204를 null로
//              처리하는 것과 같은 패턴). 그 외 실패(네트워크 오류 등)만 예외를 던집니다.
// [무엇을 돌려주는지] 로그인 상태면 { id, email, name, role } 객체, 아니면 null.
export async function fetchCurrentUser() {
  const response = await fetch("/api/auth/me");

  if (response.status === 401) {
    return null;
  }
  if (!response.ok) {
    throw new Error(`로그인 상태를 확인하지 못했습니다. (status: ${response.status})`);
  }

  return response.json();
}

// [무엇을 받아서] 아무것도 받지 않습니다.
// [무엇을 하고] POST /api/auth/logout을 호출해 로그인 쿠키를 지웁니다.
// [무엇을 돌려주는지] 아무것도 돌려주지 않습니다. 실패하면 예외를 던집니다.
export async function logout() {
  const response = await fetch("/api/auth/logout", { method: "POST" });

  if (!response.ok) {
    throw new Error(`로그아웃에 실패했습니다. (status: ${response.status})`);
  }
}
