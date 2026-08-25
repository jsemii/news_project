package com.jobnews.ai;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * [전체 흐름에서의 위치] "오늘 한 줄 요약" 기능에서 DB와 대화하는 창구입니다(쓰는 쪽).
 * NewsAnalysisMapper와 같은 ai 패키지에 두는 이유는, "언제 다시 계산할지"를 AI 구조화
 * 파이프라인(NewsStructuringService)이 결정하기 때문입니다. 조회해서 화면에 보여주는
 * 쪽은 briefing 패키지의 BriefingMapper가 별도로 담당합니다(news_analysis를 ai가
 * 쓰고 briefing이 읽는 것과 같은 구조).
 */
@Mapper
public interface DailyHighlightMapper {

    // [무엇을 받아서] 재료로 쓸 날짜(date)와 기준 점수(minScore)를 받습니다.
    // [무엇을 하고] "오늘의 브리핑"과 같은 날짜 기준(COALESCE(published_at, collected_at))으로
    //              그 날짜에 해당하고 importance_score가 기준 이상인 뉴스의 일반 요약만
    //              중요도 내림차순으로 가져옵니다.
    // [무엇을 돌려주는지] 요약 문자열 목록(뉴스 개수만큼). DailyHighlightService가 이
    //              목록의 크기로 LLM을 호출할지 말지 판단합니다.
    List<String> selectSummariesForHighlight(@Param("date") LocalDate date, @Param("minScore") int minScore);

    // [무엇을 받아서] 저장할 요약 결과(DailyHighlight) 하나를 받습니다.
    // [무엇을 하고] daily_highlight에 그 날짜 행이 없으면 새로 추가하고, 이미 있으면
    //              (하루 안에 여러 번 재계산되는 경우) 내용을 덮어씁니다 — briefing_date가
    //              기본키라서 ON CONFLICT로 멱등하게 처리됩니다.
    // [무엇을 돌려주는지] 반환값은 크게 의미 없음(영향받은 행 수).
    int upsertHighlight(DailyHighlight highlight);
}
