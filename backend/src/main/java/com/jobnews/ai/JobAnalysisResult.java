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
    // 이 뉴스가 "이 직무 하나"에 얼마나 중요한지(1~10). news_analysis.importance_score
    // (공통 점수, 3개 직무 전체 기준)와 다른 개념 — 다른 직무와 비교한 상대평가가 아니라
    // 이 직무 자체의 업무/채용 관련성만으로 매겨집니다(OpenAiClient.buildJobsSystemPrompt
    // 참고).
    private final int importanceScore;

    public JobAnalysisResult(String job, String whyItMatters, String keySkills, int importanceScore) {
        this.job = job;
        this.whyItMatters = whyItMatters;
        this.keySkills = keySkills;
        this.importanceScore = importanceScore;
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

    public int getImportanceScore() {
        return importanceScore;
    }
}
