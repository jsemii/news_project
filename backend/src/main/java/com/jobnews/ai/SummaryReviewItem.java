package com.jobnews.ai;

/**
 * [전체 흐름에서의 위치] QA 전용 조회 API(SummaryReviewController)가 돌려주는 응답
 * 항목입니다. AI가 만든 요약 품질을 사람이 직접 확인하기 위한 것이라, 기사 원문은
 * 애초에 어디에도 저장되어 있지 않아 여기 포함될 수도 없습니다(fix/no-raw-storage
 * 원칙). 원문이 궁금하면 url을 열어서 직접 확인하는 방식입니다.
 */
public class SummaryReviewItem {

    private final String title;
    private final String url;
    private final String summary;
    private final int importanceScore;

    public SummaryReviewItem(String title, String url, String summary, int importanceScore) {
        this.title = title;
        this.url = url;
        this.summary = summary;
        this.importanceScore = importanceScore;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public String getSummary() {
        return summary;
    }

    public int getImportanceScore() {
        return importanceScore;
    }
}
