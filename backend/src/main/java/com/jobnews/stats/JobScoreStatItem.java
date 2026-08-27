package com.jobnews.stats;

/**
 * [전체 흐름에서의 위치] 관리자 통계 대시보드의 "직무별 평균 중요도 점수" API가
 * 돌려주는 응답 항목입니다. news_job_analysis.importance_score(그 직무 관점에서
 * AI가 매긴 1~10점)를 직무(job)별로 평균 낸 결과 1행이 이 객체 1개에 대응합니다.
 * importance_score 컬럼은 V3 마이그레이션에서 DEFAULT 0으로 추가됐는데, 그 이전에
 * 이미 분석이 끝난 뉴스는 재분석되지 않는 한 이 0이 실제 점수처럼 영원히 남습니다
 * (analyzed 여부는 news_analysis 행 존재로만 판단하므로 — NewsAnalysisMapper.xml의
 * selectUnanalyzedNews 참고). 그래서 이 0들을 평균에 그대로 포함시키면 "1~10점
 * 척도인데 평균이 1도 안 되는" 왜곡된 숫자가 나옵니다(실측: 직무당 816건 중 583건이
 * 이 레거시 0값). StatsMapper.xml에서 importance_score > 0인 행만으로 평균을
 * 계산하고, sampleCount로 그 평균이 몇 건을 근거로 했는지 투명하게 밝힙니다.
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
    // 위 클래스 주석 참고 — 이 평균이 importance_score > 0인 몇 건을 근거로 계산됐는지.
    private final Long sampleCount;

    public JobScoreStatItem(String job, Double avgScore, Long sampleCount) {
        this.job = job;
        this.avgScore = avgScore;
        this.sampleCount = sampleCount;
    }

    public String getJob() {
        return job;
    }

    public Double getAvgScore() {
        return avgScore;
    }

    public Long getSampleCount() {
        return sampleCount;
    }
}
