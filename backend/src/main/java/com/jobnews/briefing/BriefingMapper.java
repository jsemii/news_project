package com.jobnews.briefing;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * [전체 흐름에서의 위치] "직무별 맞춤 브리핑" 조회 단계에서 DB와 대화하는 창구입니다.
 * 지금은 "오늘 분석된 뉴스 중 중요도 상위 N건"을 가져오는 조회 하나뿐입니다.
 */
@Mapper
public interface BriefingMapper {

    // [무엇을 받아서] 몇 건까지 가져올지(limit)를 받습니다.
    // [무엇을 하고] news + news_analysis를 조인하고, 오늘 분석된 것만 골라서
    //              importance_score가 높은 순으로 정렬합니다. news_industry는
    //              LEFT JOIN + string_agg로 콤마 구분 문자열 하나로 합쳐서 가져옵니다.
    // [무엇을 돌려주는지] 가공 전 행 목록(BriefingRow). List&lt;String&gt; 산업 목록으로
    //              바꾸는 것은 BriefingController가 담당합니다.
    List<BriefingRow> selectTopBriefings(int limit);
}
