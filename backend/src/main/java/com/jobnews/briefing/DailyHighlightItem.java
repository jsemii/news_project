package com.jobnews.briefing;

import java.time.LocalDate;

/**
 * [전체 흐름에서의 위치] "오늘 한 줄 요약" 조회 API(BriefingController)가 클라이언트에게
 * 돌려주는 응답 항목입니다. 그날 daily_highlight 행이 없으면(재료가 부족했거나 아직
 * 계산 전이면) 이 객체 자체가 만들어지지 않고 204 No Content로 응답합니다 — "요약 없음"은
 * 에러가 아니라 정상 상태이기 때문입니다(BriefingController의 GET /api/briefings가
 * 빈 배열로 응답하는 것과 같은 철학).
 */
public class DailyHighlightItem {

    private final LocalDate date;
    private final String headline;
    // MyBatis의 <constructor> 매핑이 JDBC INT 컬럼을 항상 boxed Integer로 취급해서
    // 생성자를 찾기 때문에, primitive int가 아니라 Integer로 선언합니다(primitive로
    // 두면 "생성자를 찾을 수 없다"는 ReflectionException이 납니다).
    private final Integer basedOnCount;

    public DailyHighlightItem(LocalDate date, String headline, Integer basedOnCount) {
        this.date = date;
        this.headline = headline;
        this.basedOnCount = basedOnCount;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getHeadline() {
        return headline;
    }

    public Integer getBasedOnCount() {
        return basedOnCount;
    }
}
