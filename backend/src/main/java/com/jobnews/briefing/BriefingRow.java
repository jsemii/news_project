package com.jobnews.briefing;

import java.time.LocalDateTime;

/**
 * [전체 흐름에서의 위치] BriefingMapper가 DB에서 조회한 결과를 그대로 담는 "가공 전"
 * 모델입니다. 산업 태그는 news_industry 테이블에 여러 행으로 나뉘어 있는데, SQL의
 * string_agg로 콤마 구분 문자열 하나로 합쳐서 가져옵니다(이 클래스의 industriesCsv).
 * 이 콤마 문자열을 실제 목록(List&lt;String&gt;)으로 쪼개서 API 응답에 알맞은 형태로
 * 바꾸는 일은 BriefingController가 합니다 — DB에서 나온 "날것" 그대로를 API 응답으로
 * 내보내지 않기 위해 이 중간 단계를 뒀습니다.
 */
public class BriefingRow {

    private Long newsId;
    private String title;
    private String url;
    private LocalDateTime publishedAt;
    private String summary;
    private int importanceScore;
    // 산업 태그들을 "금융,제조"처럼 콤마로 이어붙인 문자열. 태그가 하나도 없으면 null입니다.
    private String industriesCsv;
    // 아래는 selectTopBriefingsByJob(직무별 조회)에서만 채워집니다. 일반 모드
    // 조회(selectTopBriefings)는 이 컬럼들을 아예 select하지 않으므로 항상 null/기본값입니다.
    private String job;
    private String whyItMatters;
    private String keySkills;
    // 이 직무 관점의 중요도 점수(news_job_analysis.importance_score).
    private int jobImportanceScore;
    // 1순위(그 직무 importance_score가 briefing.job-highlight-min-score 이상)로
    // 뽑힌 뉴스면 true, 2순위(공통 점수로 나머지 채움)면 false.
    private boolean jobHighlighted;

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

    public Long getNewsId() {
        return newsId;
    }

    public void setNewsId(Long newsId) {
        this.newsId = newsId;
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

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
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

    public String getIndustriesCsv() {
        return industriesCsv;
    }

    public void setIndustriesCsv(String industriesCsv) {
        this.industriesCsv = industriesCsv;
    }

    public int getJobImportanceScore() {
        return jobImportanceScore;
    }

    public void setJobImportanceScore(int jobImportanceScore) {
        this.jobImportanceScore = jobImportanceScore;
    }

    public boolean isJobHighlighted() {
        return jobHighlighted;
    }

    public void setJobHighlighted(boolean jobHighlighted) {
        this.jobHighlighted = jobHighlighted;
    }
}
