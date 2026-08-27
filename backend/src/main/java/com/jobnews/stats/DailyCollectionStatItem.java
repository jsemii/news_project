package com.jobnews.stats;

import java.time.LocalDate;

/**
 * [전체 흐름에서의 위치] 관리자 통계 대시보드의 "최근 14일 일별 수집 건수" API가
 * 돌려주는 응답 항목입니다. 오늘을 포함한 최근 14일 전체(수집이 0건인 날짜도
 * 빠짐없이)를 항상 14행으로 돌려주므로, 프론트의 라인 차트가 항상 14개의 점을
 * 받게 됩니다(StatsMapper.xml의 generate_series 사용 이유 참고).
 */
public class DailyCollectionStatItem {

    private final LocalDate date;
    // count(n.id)는 LEFT JOIN이 매칭되지 않아도(그 날짜에 수집된 뉴스가 0건이어도)
    // 항상 값이 채워지는 컬럼이라 count(*)와 다르게 정확히 0을 셀 수 있습니다
    // (StatsMapper.xml 주석 참고). bigint → Long, 불변 객체라 primitive 대신
    // 박싱 타입을 쓰는 이유는 IndustryStatItem과 동일합니다.
    private final Long count;

    public DailyCollectionStatItem(LocalDate date, Long count) {
        this.date = date;
        this.count = count;
    }

    public LocalDate getDate() {
        return date;
    }

    public Long getCount() {
        return count;
    }
}
