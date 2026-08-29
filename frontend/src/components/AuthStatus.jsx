import { useEffect, useState } from "react";
import { fetchCurrentUser, logout } from "../api/authApi";
import { navigate } from "../hooks/useRoute";
import LoginModal from "./LoginModal";
import "./AuthStatus.css";

// [전체 흐름에서의 위치] BriefingPage 맨 위(제목보다도 위)에 놓이는 로그인 상태
// 표시줄입니다. 마운트 시 GET /api/auth/me로 로그인 상태를 확인해서, 로그인
// 상태면 이름·(관리자라면 관리자 통계 링크도)·"내 리포트 보기" 링크·로그아웃
// 버튼을, 아니면 "로그인" 버튼 하나를 보여줍니다. "내 리포트 보기"는 role과
// 무관하게 로그인만 하면 항상 보입니다(관리자 통계 링크와 달리 role 조건이
// 없음) — 비로그인 사용자에게는 아예 숨겨서 안 보이게 합니다(스크랩 버튼처럼
// "보여주되 클릭 시 로그인 유도"가 아니라, 관리자 통계 링크와 같은 "조건 안
// 맞으면 숨김" 관례를 따름 — 리포트는 스크랩이 쌓여야 의미가 생기는 화면이라
// 비로그인 사용자에게 미리 노출할 실익이 적다고 판단했습니다). "로그인" 버튼은
// 클릭하면 GitHub/Google 두 옵션이 담긴 LoginModal을 엽니다(실제 로그인 이동
// 자체는 그 모달 안의 <a href>가 담당 — OAuth2 로그인은 실제 브라우저 이동이어야
// 동작하므로 fetch로는 처리할 수 없습니다).
export default function AuthStatus() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isLoginModalOpen, setLoginModalOpen] = useState(false);

  useEffect(() => {
    let cancelled = false;

    fetchCurrentUser()
      .then((data) => {
        if (!cancelled) {
          setUser(data);
        }
      })
      .catch(() => {
        // 조용히 무시합니다: 로그인 상태 확인 실패가 브리핑 화면 자체를 막을
        // 이유는 없습니다(비로그인 상태로 취급하면 충분).
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  // [무엇을 받아서] 아무것도 받지 않습니다(버튼 클릭 이벤트에서 호출).
  // [무엇을 하고] 로그아웃 API를 호출하고, 성공하면 화면 상태를 바로 "비로그인"으로
  //              바꿉니다(새로고침 없이 즉시 로그인 버튼으로 전환).
  // [무엇을 돌려주는지] 없음.
  async function handleLogout() {
    await logout();
    setUser(null);
  }

  if (loading) {
    return <div className="auth-status" />;
  }

  return (
    <div className="auth-status">
      {user ? (
        <>
          {user.role === "ADMIN" && (
            <a
              className="auth-status__admin-link"
              href="/admin"
              onClick={(e) => {
                e.preventDefault();
                navigate("/admin");
              }}
            >
              관리자 통계
            </a>
          )}
          <a
            className="auth-status__report-link"
            href="/my-report"
            onClick={(e) => {
              e.preventDefault();
              navigate("/my-report");
            }}
          >
            내 리포트 보기
          </a>
          <span className="auth-status__name">{user.name}님</span>
          <button type="button" className="auth-status__logout" onClick={handleLogout}>
            로그아웃
          </button>
        </>
      ) : (
        <>
          <button type="button" className="auth-status__login" onClick={() => setLoginModalOpen(true)}>
            로그인/회원가입
          </button>
          <LoginModal open={isLoginModalOpen} onClose={() => setLoginModalOpen(false)} />
        </>
      )}
    </div>
  );
}
