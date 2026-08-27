// [전체 흐름에서의 위치] 이 앱 전체에서 유일하게 페이지 전환이 필요한 곳(브리핑 화면
// ↔ 관리자 통계 대시보드)을 위한 최소한의 자체 라우팅입니다. react-router 같은
// 라이브러리를 새로 추가하지 않은 이유: 새 페이지가 "/admin" 하나뿐이고 중첩 경로나
// URL 파라미터가 필요 없어서, 20~30줄짜리 History API 래퍼로 충분하기 때문입니다
// (AGENTS.md의 "불필요한 라이브러리 추가 금지" 원칙). "route"라고 부르지만 실제로는
// window.location.pathname 문자열 하나를 관찰하는 것뿐이고, 쿼리스트링/파라미터
// 파싱 같은 기능은 없습니다. 페이지가 하나 더 늘어나서 이 정도로 부족해지면 그때
// react-router 도입을 다시 검토하면 됩니다.

import { useEffect, useState } from "react";

// [무엇을 받아서] 이동할 경로(path, 예: "/admin").
// [무엇을 하고] 브라우저 주소창을 그 경로로 바꾸되 페이지를 새로 불러오지는
//              않습니다(pushState). pushState 자체는 "popstate" 이벤트를
//              발생시키지 않으므로(뒤로/앞으로 가기 버튼을 눌렀을 때만 발생),
//              usePathname을 쓰는 화면들이 이 이동을 알아챌 수 있도록 직접
//              popstate 이벤트를 만들어 쏴줍니다.
// [무엇을 돌려주는지] 없음.
export function navigate(path) {
  window.history.pushState(null, "", path);
  window.dispatchEvent(new PopStateEvent("popstate"));
}

// [무엇을 받아서] 없음.
// [무엇을 하고] 현재 주소(pathname)를 state로 들고 있다가, popstate 이벤트(뒤로/
//              앞으로 가기 버튼, 또는 위 navigate가 수동으로 쏘는 이벤트)가
//              발생할 때마다 최신 주소로 갱신합니다.
// [무엇을 돌려주는지] 현재 pathname 문자열(예: "/", "/admin"). 이 값이 바뀌면
//              이 훅을 쓰는 컴포넌트가 다시 렌더링됩니다.
export function usePathname() {
  const [pathname, setPathname] = useState(window.location.pathname);

  useEffect(() => {
    function onPopState() {
      setPathname(window.location.pathname);
    }
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  return pathname;
}
