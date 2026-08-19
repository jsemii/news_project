package com.jobnews.ai;

import java.util.List;

/**
 * [전체 흐름에서의 위치] OpenAiClient가 OpenAI의 응답을 파싱해서 만들어내는 결과
 * DTO(계층 간 데이터를 주고받기 위한 전달용 객체)입니다. 아직 news_analysis /
 * news_industry / news_job_analysis 같은 "DB에 저장할 형태"는 아니고, "AI가 이 뉴스를
 * 어떻게 분석했는지"만 담고 있습니다. 이 객체를 실제 DB 테이블 3개로 나눠 저장하는 일은
 * NewsStructuringService(Phase 5)가 담당합니다 — RssFetcher가 List&lt;News&gt;만 돌려주고
 * 저장은 NewsService가 하는 것과 같은 역할 분담입니다.
 */
public class AiAnalysisResult {

    private final String summary;
    private final List<String> industries;
    private final List<JobAnalysis> jobs;
    // 이 뉴스가 IT전산/데이터분석/백엔드 취준생에게 얼마나 중요한지 AI가 매긴 1~10점입니다.
    // news_analysis.importance_score로 그대로 저장됩니다.
    private final int importanceScore;
    // AI가 위 점수를 왜 그렇게 매겼는지 설명한 문장입니다. DB에는 저장하지 않고,
    // NewsStructuringService가 로그로만 남깁니다(점수 산정 근거를 나중에 사람이 확인할 수
    // 있도록 하기 위함이며, 별도 컬럼을 추가할 만큼 중요하진 않다고 판단했습니다).
    private final String importanceReason;

    public AiAnalysisResult(String summary, List<String> industries, List<JobAnalysis> jobs,
                             int importanceScore, String importanceReason) {
        this.summary = summary;
        this.industries = industries;
        this.jobs = jobs;
        this.importanceScore = importanceScore;
        this.importanceReason = importanceReason;
    }

    public String getSummary() {
        return summary;
    }

    public List<String> getIndustries() {
        return industries;
    }

    public List<JobAnalysis> getJobs() {
        return jobs;
    }

    public int getImportanceScore() {
        return importanceScore;
    }

    public String getImportanceReason() {
        return importanceReason;
    }

    /** 직무 하나(예: "백엔드")에 대한 AI의 분석 내용 한 조각입니다. */
    public static class JobAnalysis {
        private final String job;
        private final String whyItMatters;
        private final String keySkills;

        public JobAnalysis(String job, String whyItMatters, String keySkills) {
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
}
