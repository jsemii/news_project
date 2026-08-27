package com.jobnews.stats;

/**
 * [전체 흐름에서의 위치] 관리자 통계 대시보드의 "직무별 평균 중요도 점수" API가
 * 돌려주는 응답 항목입니다. news_job_analysis.importance_score(그 직무 관점에서
 * AI가 매긴 1~10점)를 직무(job)별로 평균 낸 결과 1행이 이 객체 1개에 대응합니다.
 */
public class JobScoreStatItem {

    private final String job;
    // AVG(...)는 PostgreSQL에서 기본적으로 numeric(정밀 소수) 타입을 돌려주는데,
    // SQL에서 ::double precision으로 명시적으로 캐스팅해서 자바 Double로 안전하게
    // 매핑되게 합니다(암묵적인 numeric→Double 매핑에 기대지 않음 — 이 프로젝트에서
    // MyBatis 타입 매핑 관련 버그를 이미 겪어봤기 때문에 명시적으로 처리합니다).
    // 불변 객체라 primitive double이 아니라 박싱된 Double을 쓰는 이유는
    // IndustryStatItem과 동일합니다(docs/troubleshooting.md 26번 항목).
    private final Double avgScore;

    public JobScoreStatItem(String job, Double avgScore) {
        this.job = job;
        this.avgScore = avgScore;
    }

    public String getJob() {
        return job;
    }

    public Double getAvgScore() {
        return avgScore;
    }
}
