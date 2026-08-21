package com.jobnews.ai;

import java.util.List;

/**
 * [전체 흐름에서의 위치] OpenAiClient의 "1단계 호출"(원문 → 일반 요약) 결과를 담는
 * DTO입니다. news_analysis(요약+중요도)와 news_industry(산업 태그)에 저장될 내용이
 * 여기 다 들어있습니다. 2단계 호출(직무별 재해석)은 원문이 아니라 이 결과의
 * summary만 입력으로 받습니다 — 원문은 1단계가 끝나는 순간 메모리에서 사라집니다.
 */
public class GeneralAnalysisResult {

    private final String summary;
    private final int importanceScore;
    private final String importanceReason;
    private final List<String> industries;

    public GeneralAnalysisResult(String summary, int importanceScore, String importanceReason,
                                  List<String> industries) {
        this.summary = summary;
        this.importanceScore = importanceScore;
        this.importanceReason = importanceReason;
        this.industries = industries;
    }

    public String getSummary() {
        return summary;
    }

    public int getImportanceScore() {
        return importanceScore;
    }

    public String getImportanceReason() {
        return importanceReason;
    }

    public List<String> getIndustries() {
        return industries;
    }
}
