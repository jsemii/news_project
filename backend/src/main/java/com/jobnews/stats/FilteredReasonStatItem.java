package com.jobnews.stats;

/**
 * [전체 흐름에서의 위치] 관리자 통계 대시보드의 "필터링 사유별 건수" API가 돌려주는
 * 응답 항목입니다. news_filtered_out.reason(규칙 기반 1차 필터가 AI 분석 대상에서
 * 제외한 이유 — ai 패키지의 FilterReason enum 값 또는 그 컬럼이 생기기 전 데이터를
 * 위한 레거시 기본값 'UNKNOWN')별로 몇 건씩 걸러졌는지 센 결과 1행이 이 객체 1개에
 * 대응합니다.
 */
public class FilteredReasonStatItem {

    private final String reason;
    // COUNT(*)의 bigint → Long 매핑, 불변 객체의 박싱 타입 사용 이유는
    // IndustryStatItem과 동일합니다.
    private final Long count;

    public FilteredReasonStatItem(String reason, Long count) {
        this.reason = reason;
        this.count = count;
    }

    public String getReason() {
        return reason;
    }

    public Long getCount() {
        return count;
    }
}
