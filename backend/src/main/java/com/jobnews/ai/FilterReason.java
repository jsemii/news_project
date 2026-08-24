package com.jobnews.ai;

/**
 * [전체 흐름에서의 위치] "AI 뉴스 구조화" 전 규칙 기반 필터(NewsRelevanceFilter)가
 * 뉴스를 걸러낸 이유를 나타냅니다. news_filtered_out.reason 컬럼에 이 값의 이름
 * (예: "TOO_OLD")이 그대로 저장됩니다. 자유 문장 대신 정해진 값만 쓰는 이유는,
 * 나중에 "얼마나 많은 뉴스가 어떤 이유로 걸러졌는지" 집계(GROUP BY reason)하거나
 * 조회할 때 문장이 조금씩 달라서 같은 이유인데도 다른 값으로 취급되는 일을 막기
 * 위해서입니다.
 */
public enum FilterReason {
    // 제목에 제외 키워드(ai.filter.exclude-title-keywords)가 포함된 경우.
    TITLE_EXCLUDED,
    // 크롤링한 원문이 없거나(크롤링 실패) 너무 짧은 경우(ai.filter.min-content-length).
    CONTENT_TOO_SHORT,
    // 수집된 지 너무 오래된(ai.structuring.max-age-days) 경우 — 시의성이 없어진 뉴스를
    // 굳이 뒤늦게 분석하지 않고 건너뛰기 위함입니다.
    TOO_OLD
}
