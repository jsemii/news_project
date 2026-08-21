package com.jobnews.ai;

/**
 * [전체 흐름에서의 위치] OpenAiClient의 "2단계 호출"(1단계 요약 → 직무별 재해석) 결과
 * 중 직무 하나에 대한 조각입니다. news_job_analysis 테이블에 뉴스 1건당 3행(직무 수만큼)
 * 저장될 내용입니다.
 */
public class JobAnalysisResult {

    private final String job;
    private final String whyItMatters;
    private final String keySkills;

    public JobAnalysisResult(String job, String whyItMatters, String keySkills) {
        this.job = job;
        this.whyItMatters = whyItMatters;
        this.keySkills = keySkills;
    }

    public String getJob() {
        return job;
    }

    public String getWhyItMatters() {
        return whyItMatters;
    }

    public String getKeySkills() {
        return keySkills;
    }
}
