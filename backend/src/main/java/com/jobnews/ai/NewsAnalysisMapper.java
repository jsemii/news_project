package com.jobnews.ai;

import com.jobnews.news.News;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * [전체 흐름에서의 위치] "AI 뉴스 구조화" 단계에서 DB와 대화하는 창구입니다.
 * news_analysis, news_industry, news_job_analysis 3개 테이블 모두 "뉴스 하나를 AI가
 * 분석한 결과"라는 하나의 개념을 나눠서 표현한 것이고, 항상 함께 만들어지고 함께
 * 쓰이기 때문에(NewsStructuringService가 한 번에 세 테이블을 다 씀), 매퍼도 하나로
 * 묶어서 관리합니다. 실제 SQL은 NewsAnalysisMapper.xml에 있습니다.
 */
@Mapper
public interface NewsAnalysisMapper {

    // [무엇을 받아서] 뉴스 id를 받습니다.
    // [무엇을 하고] news_analysis 테이블에 이미 이 뉴스에 대한 공통 분석이 있는지 확인합니다.
    // [무엇을 돌려주는지] 있으면 true, 없으면 false. NewsService.saveIfNew()의 URL 중복
    //              체크와 같은 역할을, "뉴스"가 아니라 "분석"에 대해 하는 것입니다.
    boolean existsByNewsId(long newsId);

    // [무엇을 받아서] 이번에 최대 몇 건까지 가져올지(limit)를 받습니다.
    // [무엇을 하고] news 테이블 전체 중에서, news_analysis에 아직 짝이 없는(=아직 AI
    //              분석을 안 한) 뉴스만 골라내고(news LEFT JOIN news_analysis), 오래
    //              수집된 것부터(collected_at 오름차순) limit건만 가져옵니다. 오래된
    //              것부터 처리해야, 배치를 여러 번 나눠 돌려도 특정 뉴스가 계속
    //              뒤로 밀려서 영영 분석되지 않는 일이 없습니다.
    // [무엇을 돌려주는지] 아직 분석 안 된 뉴스 중 최대 limit건. NewsStructuringService가
    //              이 목록을 순회하면서 하나씩 OpenAI에 보냅니다.
    List<News> selectUnanalyzedNews(int limit);

    // [무엇을 받아서] 입력값 없음.
    // [무엇을 하고] 아직 분석되지 않은 뉴스가 전체 몇 건 남아있는지 셉니다(limit 없이 전체 개수).
    // [무엇을 돌려주는지] 미분석 뉴스 총 건수. 배치로 나눠 처리할 때 "이번에 처리한 뒤
    //              몇 건이 더 남았는지" 알려주기 위해 사용합니다.
    int countUnanalyzedNews();

    // [무엇을 받아서] NewsRelevanceFilter가 "분석할 가치 없음"으로 판단한 뉴스의 id와,
    //              왜 걸러졌는지(FilterReason의 이름 문자열, 예: "TOO_OLD")를 받습니다.
    // [무엇을 하고] news_filtered_out에 표시를 남겨서, 이후 selectUnanalyzedNews/
    //              countUnanalyzedNews에서 이 뉴스가 다시는 조회되지 않게 합니다.
    //              (이 표시가 없으면 필터링된 뉴스가 영원히 "미분석"으로 남아 매번
    //              다시 걸러지는 문제가 있었습니다 — docs/troubleshooting.md 6번 참고.)
    //              reason을 함께 남기는 이유는 "얼마나 많은 뉴스가 어떤 이유로 걸러졌는지"
    //              나중에 추적할 수 있게 하기 위함입니다(18번 항목 참고).
    // [무엇을 돌려주는지] 반환값은 크게 의미 없음(영향받은 행 수).
    int insertFilteredOut(@Param("newsId") long newsId, @Param("reason") String reason);

    // [무엇을 받아서] 저장할 공통 분석(NewsAnalysis) 하나를 받습니다.
    // [무엇을 하고] news_analysis 테이블에 1행을 추가합니다.
    int insertAnalysis(NewsAnalysis analysis);

    // [무엇을 받아서] 저장할 산업 태그 목록(NewsIndustry, 보통 1~3개)을 받습니다.
    // [무엇을 하고] news_industry 테이블에 여러 행을 한 번에 추가합니다(SQL 한 번으로 여러
    //              행을 넣는 배치 insert — 산업 개수만큼 DB를 여러 번 왕복하지 않기 위함).
    int insertIndustries(List<NewsIndustry> industries);

    // [무엇을 받아서] 저장할 직무별 분석 목록(NewsJobAnalysis, 항상 3개 — IT전산/데이터분석/백엔드).
    // [무엇을 하고] news_job_analysis 테이블에 여러 행을 한 번에 추가합니다(위와 같은 이유의 배치 insert).
    int insertJobAnalyses(List<NewsJobAnalysis> jobAnalyses);
}
