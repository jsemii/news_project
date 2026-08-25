package com.jobnews.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * [전체 흐름에서의 위치] "오늘 한 줄 요약" 기능 전체를 지휘하는 오케스트레이터입니다.
 * NewsStructuringService가 배치 하나를 끝낼 때마다(실제로 뭔가 새로 분석됐을 때만)
 * recomputeForToday()를 호출합니다. 그날 importance_score가 기준 이상인 뉴스의 요약을
 * 모아서, 재료가 너무 적으면 LLM을 아예 부르지 않고(억지 연결 방지), 충분하면
 * OpenAiClient에게 공통 흐름을 뽑게 한 뒤 daily_highlight에 저장(멱등한 UPSERT)합니다.
 */
@Service
public class DailyHighlightService {

    private static final Logger log = LoggerFactory.getLogger(DailyHighlightService.class);

    private final DailyHighlightMapper dailyHighlightMapper;
    private final OpenAiClient openAiClient;
    private final DailyHighlightProperties dailyHighlightProperties;

    public DailyHighlightService(DailyHighlightMapper dailyHighlightMapper,
                                  OpenAiClient openAiClient,
                                  DailyHighlightProperties dailyHighlightProperties) {
        this.dailyHighlightMapper = dailyHighlightMapper;
        this.openAiClient = openAiClient;
        this.dailyHighlightProperties = dailyHighlightProperties;
    }

    // [무엇을 받아서] 입력값 없음(항상 "오늘" 기준으로 재계산).
    // [무엇을 하고] 오늘 기준 점수(daily-highlight.min-importance-score) 이상인 뉴스의
    //              요약을 모읍니다. 건수가 daily-highlight.min-news-count 미만이면
    //              (여러 뉴스를 관통하는 "흐름"이라는 개념 자체가 성립하기 어려운 건수라)
    //              LLM을 호출하지 않고 조용히 끝냅니다 — 2건짜리 뉴스를 놓고 억지 공통점을
    //              짜낼 위험을 코드 레벨에서 차단하는 것이 핵심입니다. 충분하면 LLM을
    //              불러 한 문장을 받고, daily_highlight에 그 날짜 행을 UPSERT합니다.
    // [무엇을 돌려주는지] 없음. 실패해도(LLM 호출 실패 등) 배치 전체를 죽이지 않도록
    //              NewsStructuringService 쪽에서 이 메서드 호출을 감싸서 처리합니다.
    public void recomputeForToday() {
        LocalDate today = LocalDate.now();
        List<String> summaries = dailyHighlightMapper.selectSummariesForHighlight(
                today, dailyHighlightProperties.getMinImportanceScore());

        if (summaries.size() < dailyHighlightProperties.getMinNewsCount()) {
            log.debug("Skipping daily highlight for {}: only {} news >= {} points (min {})",
                    today, summaries.size(), dailyHighlightProperties.getMinImportanceScore(),
                    dailyHighlightProperties.getMinNewsCount());
            return;
        }

        String headline = openAiClient.analyzeDailyHighlight(summaries);
        dailyHighlightMapper.upsertHighlight(new DailyHighlight(today, headline, summaries.size()));
        log.info("Daily highlight recomputed for {}: basedOnCount={}", today, summaries.size());
    }
}
