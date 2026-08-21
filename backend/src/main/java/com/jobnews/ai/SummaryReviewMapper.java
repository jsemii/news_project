package com.jobnews.ai;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * [전체 흐름에서의 위치] QA 전용 조회 API가 DB와 대화하는 창구입니다. news와
 * news_analysis를 news_id로 join해서, AI가 만든 요약 품질을 개발 중에 눈으로
 * 확인할 수 있는 최소한의 데이터만 가져옵니다.
 */
@Mapper
public interface SummaryReviewMapper {

    // [무엇을 받아서] 입력값 없음.
    // [무엇을 하고] news와 news_analysis를 news_id로 join해서, 수집 시각(collected_at)이
    //              최신인 순서로 정렬한 뒤 상위 50건만 가져옵니다. 50은 "개발 중 확인용,
    //              단순하게"라는 요구사항에 맞춰 설정값으로 빼지 않고 SQL에 고정했습니다
    //              (briefing.top-n처럼 운영 중 조정할 필요가 없는 값이라고 판단).
    // [무엇을 돌려주는지] 최근 분석된 뉴스 50건(또는 그 미만)의 title/url/summary/importanceScore.
    List<SummaryReviewRow> selectRecentSummaries();
}
