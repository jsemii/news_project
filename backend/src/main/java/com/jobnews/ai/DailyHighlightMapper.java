package com.jobnews.ai;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * [전체 흐름에서의 위치] "오늘 한 줄 요약" 기능에서 DB와 대화하는 창구입니다(쓰는 쪽).
 * NewsAnalysisMapper와 같은 ai 패키지에 두는 이유는, "언제 다시 계산할지"를 독립
 * 스케줄러(DailyHighlightScheduler)가 결정하기 때문입니다. 조회해서 화면에 보여주는
 * 쪽은 briefing 패키지의 BriefingMapper가 별도로 담당합니다(news_analysis를 ai가
 * 쓰고 briefing이 읽는 것과 같은 구조).
 */
@Mapper
public interface DailyHighlightMapper {

    // [무엇을 받아서] 재료로 쓸 날짜(date)와 최대 건수(limit)를 받습니다.
    // [무엇을 하고] BriefingMapper.selectTopBriefings와 완전히 같은 모집단 정의(같은
    //              날짜 기준 COALESCE(published_at, collected_at) + importance_score
    //              내림차순 + limit)로 뉴스의 일반 요약만 가져옵니다 — "오늘의 브리핑"
    //              화면에 실제로 보이는 뉴스와 항상 정확히 같은 집합을 재료로 쓰기
    //              위함입니다(예전엔 점수 임계값만 썼는데, 화면에 안 보이는 뉴스가
    //              요약 재료에 섞이거나 반대로 빠지는 불일치가 있었습니다).
    // [무엇을 돌려주는지] 요약 문자열 목록(최대 limit개). DailyHighlightService가 이
    //              목록의 크기로 LLM을 호출할지 말지 판단합니다.
    List<String> selectSummariesForHighlight(@Param("date") LocalDate date, @Param("limit") int limit);

    // [무엇을 받아서] 저장할 요약 결과(DailyHighlight) 하나를 받습니다.
    // [무엇을 하고] daily_highlight에 그 날짜 행이 없으면 새로 추가하고, 이미 있으면
    //              (하루 안에 여러 번 재계산되는 경우) 내용을 덮어씁니다 — briefing_date가
    //              기본키라서 ON CONFLICT로 멱등하게 처리됩니다.
    // [무엇을 돌려주는지] 반환값은 크게 의미 없음(영향받은 행 수).
    int upsertHighlight(DailyHighlight highlight);

    // [무엇을 받아서] 입력값 없음.
    // [무엇을 하고] news_analysis가 존재하는(=AI 구조화가 최소 1건이라도 끝난) 뉴스들의
    //              "오늘의 브리핑" 날짜 기준(COALESCE(published_at, collected_at))을
    //              중복 없이 모읍니다 — 뉴스가 수집되기 시작한 이후 하루라도 뉴스가 있었던
    //              날짜 전체입니다.
    // [무엇을 돌려주는지] 날짜 오름차순 목록. DailyHighlightService.backfillMissing()이
    //              이 목록에서 아직 daily_highlight가 없는 날짜만 골라 계산합니다.
    List<LocalDate> selectDistinctBriefingDates();

    // [무엇을 받아서] 입력값 없음.
    // [무엇을 하고] daily_highlight에 이미 값이 있는 날짜를 전부 가져옵니다.
    // [무엇을 돌려주는지] 이미 계산된 날짜 목록(순서 무관 — 호출하는 쪽이 Set으로 바꿔 사용).
    List<LocalDate> selectExistingHighlightDates();
}
