package com.jobnews.stats;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * [전체 흐름에서의 위치] 관리자 통계 대시보드 조회 단계에서 DB와 대화하는
 * 창구입니다. 4개 지표(산업별 건수/일별 수집 건수/직무별 평균 점수/필터링 사유별
 * 건수) 모두 파라미터 없이 전체 데이터를 집계(GROUP BY)하는 단순 조회라, briefing
 * 패키지처럼 별도 Service 계층 없이 StatsController가 이 Mapper를 직접 호출합니다.
 */
@Mapper
public interface StatsMapper {

    // [무엇을 받아서] 파라미터 없음(전체 기간 집계).
    // [무엇을 하고] news_industry를 industry별로 묶어 건수를 셉니다.
    // [무엇을 돌려주는지] 산업별 건수 목록(건수 내림차순 — StatsMapper.xml 참고).
    List<IndustryStatItem> selectIndustryCounts();

    // [무엇을 받아서] 파라미터 없음(항상 "오늘 포함 최근 14일" 고정 범위).
    // [무엇을 하고] news를 collected_at 날짜별로 묶어 건수를 셉니다. 수집이 0건인
    //              날짜도 빠뜨리지 않고 항상 14행을 돌려줍니다(StatsMapper.xml의
    //              generate_series 사용 이유 참고).
    // [무엇을 돌려주는지] 날짜 오름차순 14개 항목의 목록.
    List<DailyCollectionStatItem> selectDailyCollectionCounts();

    // [무엇을 받아서] 파라미터 없음.
    // [무엇을 하고] news_job_analysis를 job별로 묶어 importance_score의 평균을
    //              냅니다. importance_score = 0(V3 마이그레이션 이전 레거시
    //              기본값, 실제 분석 점수가 아님)인 행은 평균 계산에서 제외합니다
    //              (JobScoreStatItem 클래스 주석 참고).
    // [무엇을 돌려주는지] 직무별 평균 점수 목록(평균 점수 내림차순), 각 항목에는
    //              그 평균이 몇 건을 근거로 했는지(sampleCount)도 포함됩니다.
    List<JobScoreStatItem> selectJobAverageScores();

    // [무엇을 받아서] 파라미터 없음.
    // [무엇을 하고] news_filtered_out을 reason별로 묶어 건수를 셉니다.
    // [무엇을 돌려주는지] 필터링 사유별 건수 목록(건수 내림차순).
    List<FilteredReasonStatItem> selectFilteredReasonCounts();
}
