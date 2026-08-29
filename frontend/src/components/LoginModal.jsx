import { useEffect, useRef } from "react";
import "./LoginModal.css";

// [전체 흐름에서의 위치] AuthStatus의 "로그인" 버튼을 눌렀을 때 뜨는 모달입니다.
// GitHub/Google 로그인 옵션 두 개를 보여주는 것 외에 다른 역할은 없습니다. 네이티브
// <dialog> 엘리먼트의 showModal()/close()를 그대로 쓰는 이유: Escape 키로 닫기,
// 포커스를 모달 안에 가두기(포커스 트랩), 닫을 때 원래 포커스가 있던 곳(로그인
// 버튼)으로 돌아가기, 배경 어둡게 하기(::backdrop)가 전부 브라우저 기본 동작으로
// 해결돼서, 직접 오버레이를 만드는 것보다 코드가 훨씬 적게 듭니다.
//
// [무엇을 받아서] open(모달을 열지 여부), onClose(모달이 닫힐 때 호출할 콜백 —
//              Escape/배경 클릭/닫기 버튼 전부 이 콜백 하나로 귀결됩니다), message
//              (선택, 제목 아래에 보여줄 안내 문구 — 예: 로그인이 필요한 동작을
//              하려다 이 모달이 뜬 경우 "로그인 후 이용하십시오"처럼 왜 떴는지
//              설명. 생략하면 아무것도 안 보여줌).
// [무엇을 하고] open 값이 바뀔 때마다 실제 <dialog> DOM을 그 값에 맞게
//              showModal()/close()로 동기화합니다. <dialog>가 어떤 방식으로든
//              닫히면(Escape 포함) 브라우저가 "close" 이벤트를 쏴주므로, 그
//              이벤트 하나만 구독해서 React 쪽 상태(부모의 open state)를
//              되돌립니다 — 닫히는 경로가 여러 개(Escape/배경 클릭/버튼)여도
//              동기화 지점은 한 곳뿐입니다.
// [무엇을 돌려주는지] 모달 UI(JSX). open이 false여도 컴포넌트 자체는 항상
//              마운트돼 있습니다(<dialog>가 열려있지 않을 뿐).
export default function LoginModal({ open, onClose, message }) {
  const dialogRef = useRef(null);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (open && !dialog.open) {
      dialog.showModal();
    } else if (!open && dialog.open) {
      dialog.close();
    }
  }, [open]);

  useEffect(() => {
    const dialog = dialogRef.current;
    dialog.addEventListener("close", onClose);
    return () => dialog.removeEventListener("close", onClose);
  }, [onClose]);

  // [무엇을 받아서] <dialog> 자신에서 발생한 클릭 이벤트.
  // [무엇을 하고] 배경(::backdrop) 클릭 시 event.target이 <dialog> 엘리먼트
  //              자신이 되는 표준 동작을 이용해, "패널 바깥을 클릭했을 때만"
  //              닫습니다. 패널 내부 클릭은 패널 div의 onClick에서
  //              stopPropagation으로 미리 막아서 여기까지 전파되지 않습니다.
  // [무엇을 돌려주는지] 없음.
  function handleBackdropClick(e) {
    if (e.target === dialogRef.current) {
      dialogRef.current.close();
    }
  }

  return (
    <dialog ref={dialogRef} className="login-modal" onClick={handleBackdropClick}>
      <div className="login-modal__panel" onClick={(e) => e.stopPropagation()}>
        <button
          type="button"
          className="login-modal__close"
          aria-label="닫기"
          onClick={() => dialogRef.current.close()}
        >
          ×
        </button>
        <h2 className="login-modal__title">로그인</h2>
        {message && <p className="login-modal__message">{message}</p>}
        <div className="login-modal__providers">
          <a className="login-modal__provider" href="/oauth2/authorization/github">
            GitHub로 로그인
          </a>
          <a className="login-modal__provider" href="/oauth2/authorization/google">
            Google로 로그인
          </a>
        </div>
      </div>
    </dialog>
  );
}
