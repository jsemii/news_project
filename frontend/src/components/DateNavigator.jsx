import { useRef } from "react";
import { getTodayString, shiftDateString } from "../utils/dateUtils";
import "./DateNavigator.css";

// [무엇을 받아서] "yyyy-MM-dd" 날짜 문자열.
// [무엇을 하고] 오늘이면 "오늘", 아니면 "8월 15일" 형식으로 바꿉니다(연도는 화면이
//              복잡해지지 않게 생략 — 어차피 과거로 갈 수 있는 범위가 넓지 않은 서비스입니다).
// [무엇을 돌려주는지] 화면에 보여줄 날짜 라벨 문자열.
function formatLabel(dateString) {
  if (dateString === getTodayString()) {
    return "오늘";
  }
  const [, month, day] = dateString.split("-").map(Number);
  return `${month}월 ${day}일`;
}

// [전체 흐름에서의 위치] BriefingPage 상단에 놓이는 날짜 이동 UI입니다. JobSelector와
// 마찬가지로 자기만의 상태를 갖지 않는 "순수 프레젠테이션 컴포넌트"입니다 — 현재 선택된
// 날짜(selectedDate)와, 날짜가 바뀌었을 때 무엇을 할지(onChange)를 모두 부모
// (BriefingPage)로부터 props로 받습니다.
export default function DateNavigator({ selectedDate, onChange }) {
  const dateInputRef = useRef(null);
  const today = getTodayString();
  // "yyyy-MM-dd" 형식은 사전식(문자열) 비교 순서가 실제 날짜 순서와 똑같기 때문에,
  // Date 객체로 바꾸지 않고 문자열 그대로 비교해도 안전합니다.
  const isToday = selectedDate >= today;

  // [무엇을 하고] 화면엔 작은 달력 아이콘 버튼만 보이지만, 실제로는 바로 옆에 숨겨둔
  //              <input type="date">의 네이티브 날짜 선택 UI를 열어줍니다. 이렇게 하는
  //              이유: 브라우저 기본 달력 팝업을 그대로 재사용하면 새 캘린더 라이브러리를
  //              추가할 필요가 없습니다. showPicker()를 지원하지 않는(오래된) 브라우저에서는
  //              대신 포커스만 줘서, 사용자가 키보드로도 날짜를 입력할 수 있게 합니다.
  function openCalendar() {
    const input = dateInputRef.current;
    if (!input) return;
    if (typeof input.showPicker === "function") {
      input.showPicker();
    } else {
      input.focus();
    }
  }

  return (
    <div className="date-navigator">
      <button
        type="button"
        className="date-navigator__arrow"
        aria-label="하루 전"
        onClick={() => onChange(shiftDateString(selectedDate, -1))}
      >
        ◀
      </button>

      <span className="date-navigator__label">{formatLabel(selectedDate)}</span>

      <span className="date-navigator__calendar-wrap">
        <button
          type="button"
          className="date-navigator__calendar-button"
          aria-label="날짜 선택"
          onClick={openCalendar}
        >
          📅
        </button>
        <input
          ref={dateInputRef}
          type="date"
          className="date-navigator__calendar-input"
          value={selectedDate}
          max={today}
          onChange={(e) => {
            if (e.target.value) onChange(e.target.value);
          }}
          aria-hidden="true"
          tabIndex={-1}
        />
      </span>

      <button
        type="button"
        className="date-navigator__arrow"
        aria-label="하루 다음"
        disabled={isToday}
        onClick={() => onChange(shiftDateString(selectedDate, 1))}
      >
        ▶
      </button>
    </div>
  );
}
