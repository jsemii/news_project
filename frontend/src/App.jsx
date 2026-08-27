import { lazy, Suspense } from "react";
import BriefingPage from "./pages/BriefingPage";
import { usePathname } from "./hooks/useRoute";

// AdminStatsPage는 recharts(차트 라이브러리)를 통째로 불러오는데, 이 서비스를
// 찾는 사람 대부분(구직자)은 이 페이지를 평생 볼 일이 없는 관리자 전용
// 화면입니다. lazy()로 이 페이지의 코드(+recharts)를 별도 청크로 분리해두면,
// 일반 사용자는 브리핑 화면만 내려받고 "/admin"에 실제로 들어간 사람만 그
// 청크를 추가로 내려받습니다.
const AdminStatsPage = lazy(() => import("./pages/AdminStatsPage"));

function App() {
  const pathname = usePathname();

  if (pathname === "/admin") {
    return (
      <Suspense fallback={null}>
        <AdminStatsPage />
      </Suspense>
    );
  }

  return <BriefingPage />;
}

export default App;
