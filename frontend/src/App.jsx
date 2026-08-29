import { lazy, Suspense } from "react";
import BriefingPage from "./pages/BriefingPage";
import { usePathname } from "./hooks/useRoute";

// AdminStatsPage/MyReportPage 둘 다 recharts(차트 라이브러리)를 통째로 불러오는데,
// 그 화면을 실제로 들어가는 사람만 일부입니다(AdminStatsPage는 관리자만, MyReportPage는
// 로그인해서 "내 리포트 보기"를 누른 사람만). lazy()로 각 페이지의 코드(+recharts)를
// 별도 청크로 분리해두면, 그냥 브리핑만 보는 방문자는 이 무거운 코드를 전혀 안
// 내려받습니다.
const AdminStatsPage = lazy(() => import("./pages/AdminStatsPage"));
const MyReportPage = lazy(() => import("./pages/MyReportPage"));

function App() {
  const pathname = usePathname();

  if (pathname === "/admin") {
    return (
      <Suspense fallback={null}>
        <AdminStatsPage />
      </Suspense>
    );
  }

  if (pathname === "/my-report") {
    return (
      <Suspense fallback={null}>
        <MyReportPage />
      </Suspense>
    );
  }

  return <BriefingPage />;
}

export default App;
