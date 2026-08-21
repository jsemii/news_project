package com.jobnews.ai;

import java.util.List;

/**
 * [전체 흐름에서의 위치] SummaryReviewMapper가 DB에서 조회한 결과를 담는 "가공 전"
 * 모델입니다(briefing 패키지의 BriefingRow와 같은 역할). MyBatis는 기본 생성자 +
 * setter가 있는 객체에 값을 채우기 편해서, API 응답용 불변 객체(SummaryReviewItem)와
 * 분리해뒀습니다. SummaryReviewController가 이 Row를 Item으로 바꿔서 응답합니다.
 * jobAnalyses는 이 뉴스에 대한 직무별(IT전산/데이터분석/백엔드) 재해석 목록으로,
 * MyBatis가 news_job_analysis를 news_id로 join해서 <collection>으로 채워줍니다.
 */
public class SummaryReviewRow {

    private Long newsId;
    private String title;
    private String url;
    private String summary;
    private int importanceScore;
    private List<JobAnalysisReviewRow> jobAnalyses;

    public Long getNewsId() {
        return newsId;
    }

    public void setNewsId(Long newsId) {
        this.newsId = newsId;
    }

    public List<JobAnalysisReviewRow> getJobAnalyses() {
        return jobAnalyses;
    }

    public void setJobAnalyses(List<JobAnalysisReviewRow> jobAnalyses) {
        this.jobAnalyses = jobAnalyses;
    }

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
