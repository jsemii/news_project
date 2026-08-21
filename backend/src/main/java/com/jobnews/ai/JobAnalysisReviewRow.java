package com.jobnews.ai;

/**
 * [전체 흐름에서의 위치] SummaryReviewMapper가 news_job_analysis에서 조회한 직무별
 * 재해석 한 건을 담는 "가공 전" 모델입니다(SummaryReviewRow의 jobAnalyses 목록 안에
 * 여러 개 들어갑니다). MyBatis의 <collection> 매핑은 이렇게 setter가 있는 mutable
 * 객체에 값을 채우는 방식이라 JobAnalysisResult(불변, API 응답용)와 분리해뒀습니다.
 */
public class JobAnalysisReviewRow {

    private String job;
    private String whyItMatters;
    private String keySkills;

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public String getWhyItMatters() {
        return whyItMatters;
    }

    public void setWhyItMatters(String whyItMatters) {
        this.whyItMatters = whyItMatters;
    }

    public String getKeySkills() {
        return keySkills;
    }

    public void setKeySkills(String keySkills) {
        this.keySkills = keySkills;
    }
}
