package com.jobnews.ai;

/**
 * [전체 흐름에서의 위치] SummaryReviewMapper가 DB에서 조회한 결과를 담는 "가공 전"
 * 모델입니다(briefing 패키지의 BriefingRow와 같은 역할). MyBatis는 기본 생성자 +
 * setter가 있는 객체에 값을 채우기 편해서, API 응답용 불변 객체(SummaryReviewItem)와
 * 분리해뒀습니다. SummaryReviewController가 이 Row를 Item으로 바꿔서 응답합니다.
 */
public class SummaryReviewRow {

    private String title;
    private String url;
    private String summary;
    private int importanceScore;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public int getImportanceScore() {
        return importanceScore;
    }

    public void setImportanceScore(int importanceScore) {
        this.importanceScore = importanceScore;
    }
}
