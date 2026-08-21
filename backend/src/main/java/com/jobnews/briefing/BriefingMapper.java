package com.jobnews.briefing;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * [전체 흐름에서의 위치] "직무별 맞춤 브리핑" 조회 단계에서 DB와 대화하는 창구입니다.
 * "특정 날짜에 분석된 뉴스 중 중요도 상위 N건"을 가져오는 조회 두 가지(일반 모드/
 * 직무별 모드)가 있습니다.
 */
@Mapper
public interface BriefingMapper {

    // [무엇을 받아서] 어느 날짜에 분석된 것을 볼지(date)와 몇 건까지 가져올지(limit)를 받습니다.
    // [무엇을 하고] news + news_analysis를 조인하고, 그 날짜에 분석된 것만 골라서
    //              importance_score가 높은 순으로 정렬합니다. news_industry는
    //              LEFT JOIN + string_agg로 콤마 구분 문자열 하나로 합쳐서 가져옵니다.
    // [무엇을 돌려주는지] 가공 전 행 목록(BriefingRow). List&lt;String&gt; 산업 목록으로
    //              바꾸는 것은 BriefingController가 담당합니다.
    // @Param: 파라미터가 2개 이상이면 MyBatis가 XML의 #{date}/#{limit}을 실제 어느
    // 인자와 연결할지 이름만으로는 알 수 없어서, @Param으로 이름을 명시적으로 붙여줘야 합니다.
    List<BriefingRow> selectTopBriefings(@Param("date") LocalDate date, @Param("limit") int limit);

    // [무엇을 받아서] 어느 직무(job: "IT전산"/"데이터분석"/"백엔드" 중 하나) 관점으로
    //              볼지, 어느 날짜에 분석된 것을 볼지(date), 몇 건까지 가져올지(limit)를 받습니다.
    // [무엇을 하고] selectTopBriefings와 같은 조건(그 날짜에 분석된 것, importance_score
    //              내림차순)에 news_job_analysis를 job으로 추가 조인해서, 각 행에
    //              그 직무 관점의 재해석(job/whyItMatters/keySkills)까지 채워 옵니다.
    // [무엇을 돌려주는지] job/whyItMatters/keySkills가 채워진 BriefingRow 목록.
    List<BriefingRow> selectTopBriefingsByJob(@Param("job") String job, @Param("date") LocalDate date,
                                               @Param("limit") int limit);
}
