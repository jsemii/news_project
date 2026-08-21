import "./JobSelector.css";

// 화면에 보여줄 탭 목록입니다. value가 null이면 "일반 모드"(직무 필터 없음)를 뜻하고,
// 백엔드 GET /api/briefings 호출 시 job 파라미터를 아예 안 붙입니다. 나머지 3개는
// application.yml의 ai.jobs(IT전산/데이터분석/백엔드)와 이름을 똑같이 맞춰야 백엔드가
// 인식합니다(다르면 400 에러).
const JOB_TABS = [
  { value: null, label: "전체" },
  { value: "IT전산", label: "IT전산" },
  { value: "데이터분석", label: "데이터분석" },
  { value: "백엔드", label: "백엔드" },
];

// [전체 흐름에서의 위치] BriefingPage 상단에 놓이는 직무 선택 탭입니다. 이 컴포넌트는
// 자기만의 상태를 갖지 않는 "순수 프레젠테이션 컴포넌트"입니다 — 어떤 탭이 선택돼
// 있는지(selectedJob)와, 사용자가 탭을 클릭했을 때 무엇을 할지(onSelect)를 모두
// 부모(BriefingPage)로부터 props로 받아서 화면에 그리기만 합니다. 실제 데이터를
// 다시 불러오는 로직은 BriefingPage의 useEffect가 담당합니다.
export default function JobSelector({ selectedJob, onSelect }) {
  return (
    <div className="job-selector" role="tablist" aria-label="관심 직무 선택">
      {JOB_TABS.map((tab) => {
        const isActive = tab.value === selectedJob;
        return (
          <button
            key={tab.label}
            type="button"
            role="tab"
            aria-selected={isActive}
            className={`job-selector__tab${isActive ? " job-selector__tab--active" : ""}`}
            onClick={() => onSelect(tab.value)}
          >
            {tab.label}
          </button>
        );
      })}
    </div>
  );
}
