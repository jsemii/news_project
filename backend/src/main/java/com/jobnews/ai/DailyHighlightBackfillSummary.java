package com.jobnews.ai;

/**
 * [전체 흐름에서의 위치] DailyHighlightService.backfillMissing()을 한 번 실행한
 * 결과를 요약한 DTO입니다. 수동 트리거 API(StructuringController)가 호출자에게
 * "몇 개 날짜를 훑었고 몇 개를 새로 계산했는지"를 바로 알려주기 위해 사용합니다.
 * StructuringSummary와 같은 목적의 클래스를 "오늘 한 줄 요약" 소급 채우기 전용으로
 * 별도로 둔 이유는, 다루는 대상이 "뉴스 건수"가 아니라 "날짜 개수"라 의미가 달라서입니다.
 */
public class DailyHighlightBackfillSummary {

    private final int totalDates;
    private final int computed;
    private final int skippedTooFew;

    public DailyHighlightBackfillSummary(int totalDates, int computed, int skippedTooFew) {
        this.totalDates = totalDates;
        this.computed = computed;
        this.skippedTooFew = skippedTooFew;
    }

    public int getTotalDates() {
        return totalDates;
    }

    public int getComputed() {
        return computed;
    }

    public int getSkippedTooFew() {
        return skippedTooFew;
    }
}
