package com.jobnews.ai;

import java.util.List;

/**
 * [전체 흐름에서의 위치] QA 전용 조회 API(SummaryReviewController)가 돌려주는 응답
 * 항목입니다. AI가 만든 요약 품질을 사람이 직접 확인하기 위한 것이라, 기사 원문은
 * 애초에 어디에도 저장되어 있지 않아 여기 포함될 수도 없습니다(fix/no-raw-storage
 * 원칙). 원문이 궁금하면 url을 열어서 직접 확인하는 방식입니다.
 * jobAnalyses는 같은 뉴스를 IT전산/데이터분석/백엔드 3가지 직무 관점으로 재해석한
 * 결과입니다(2단계 LLM 호출 결과와 같은 모양이라 JobAnalysisResult를 그대로 씁니다).
 */
public class SummaryReviewItem {

    private final String title;
    private final String url;
    private final String summary;
    private final int importanceScore;
    private final List<JobAnalysisResult> jobAnalyses;

    public SummaryReviewItem(String title, String url, String summary, int importanceScore,
                              List<JobAnalysisResult> jobAnalyses) {
        this.title = title;
        this.url = url;
        this.summary = summary;
        this.importanceScore = importanceScore;
        this.jobAnalyses = jobAnalyses;
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

    public List<JobAnalysisResult> getJobAnalyses() {
        return jobAnalyses;
    }
}
