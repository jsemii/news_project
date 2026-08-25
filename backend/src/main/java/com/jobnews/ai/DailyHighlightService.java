package com.jobnews.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    // [무엇을 받아서] 입력값 없음(항상 "오늘" 기준).
    // [무엇을 하고] recompute(LocalDate.now())를 그대로 호출합니다. NewsStructuringService가
    //              배치 하나를 끝낼 때마다 부르는 진입점입니다.
    // [무엇을 돌려주는지] 없음.
    public void recomputeForToday() {
        recompute(LocalDate.now());
    }

    // [무엇을 받아서] 재계산할 날짜.
    // [무엇을 하고] 그 날짜 기준 점수(daily-highlight.min-importance-score) 이상인 뉴스의
    //              요약을 모읍니다. 건수가 daily-highlight.min-news-count 미만이면
    //              (여러 뉴스를 관통하는 "흐름"이라는 개념 자체가 성립하기 어려운 건수라)
    //              LLM을 호출하지 않고 조용히 끝냅니다 — 2건짜리 뉴스를 놓고 억지 공통점을
    //              짜낼 위험을 코드 레벨에서 차단하는 것이 핵심입니다. 충분하면 LLM을
    //              불러 한 문장을 받고, daily_highlight에 그 날짜 행을 UPSERT합니다.
    // [무엇을 돌려주는지] 실제로 계산해서 저장했으면 true, 재료가 부족해 건너뛰었으면
    //              false — backfillMissing()이 이 값으로 진행 상황을 집계합니다.
    public boolean recompute(LocalDate date) {
        List<String> summaries = dailyHighlightMapper.selectSummariesForHighlight(
                date, dailyHighlightProperties.getMinImportanceScore());

        if (summaries.size() < dailyHighlightProperties.getMinNewsCount()) {
            log.debug("Skipping daily highlight for {}: only {} news >= {} points (min {})",
                    date, summaries.size(), dailyHighlightProperties.getMinImportanceScore(),
                    dailyHighlightProperties.getMinNewsCount());
            return false;
        }

        String headline = openAiClient.analyzeDailyHighlight(summaries);
        dailyHighlightMapper.upsertHighlight(new DailyHighlight(date, headline, summaries.size()));
        log.info("Daily highlight recomputed for {}: basedOnCount={}", date, summaries.size());
        return true;
    }

    // [무엇을 받아서] 입력값 없음.
    // [무엇을 하고] 뉴스가 수집되기 시작한 이후 "분석이 하나라도 있는 날짜" 전체를 훑어서,
    //              daily_highlight에 아직 행이 없는 날짜만 골라 recompute(date)를
    //              호출합니다. 이미 계산된 날짜는 다시 계산하지 않습니다(불필요한 OpenAI
    //              호출 방지) — 사람이 "옛날 날짜도 다 보이게 해달라"고 요청했을 때
    //              한 번 실행하는 소급 채우기(backfill) 용도입니다.
    // [무엇을 돌려주는지] 이번 실행에서 몇 개 날짜를 훑었고, 몇 개를 새로 계산했고, 몇 개를
    //              재료 부족으로 건너뛰었는지 요약.
    public DailyHighlightBackfillSummary backfillMissing() {
        List<LocalDate> allDates = dailyHighlightMapper.selectDistinctBriefingDates();
        Set<LocalDate> alreadyComputed = new HashSet<>(dailyHighlightMapper.selectExistingHighlightDates());

        int computed = 0;
        int skipped = 0;
        for (LocalDate date : allDates) {
            if (alreadyComputed.contains(date)) {
                continue;
            }
            if (recompute(date)) {
                computed++;
            } else {
                skipped++;
            }
        }

        return new DailyHighlightBackfillSummary(allDates.size(), computed, skipped);
    }
}
