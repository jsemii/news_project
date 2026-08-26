package com.jobnews.ai;

import com.jobnews.briefing.BriefingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * [전체 흐름에서의 위치] "오늘 한 줄 요약" 기능 전체를 지휘하는 오케스트레이터입니다.
 * DailyHighlightScheduler가 하루 3번(고정 시각) recomputeForToday()를 호출합니다 —
 * 뉴스 구조화 배치(매시간)와는 완전히 분리된 독립 스케줄입니다(비용 예측 가능성을
 * 위해 이렇게 분리했습니다). "오늘의 브리핑"(BriefingMapper.selectTopBriefings)과
 * 정확히 같은 모집단(그날 중요도 상위 briefing.top-n건)의 요약을 모아서, 재료가 너무
 * 적으면 LLM을 아예 부르지 않고(억지 연결 방지), 충분하면 OpenAiClient에게 공통 흐름을
 * 뽑게 한 뒤 daily_highlight에 저장(멱등한 UPSERT)합니다.
 */
@Service
public class DailyHighlightService {

    private static final Logger log = LoggerFactory.getLogger(DailyHighlightService.class);

    private final DailyHighlightMapper dailyHighlightMapper;
    private final OpenAiClient openAiClient;
    private final DailyHighlightProperties dailyHighlightProperties;
    private final BriefingProperties briefingProperties;

    public DailyHighlightService(DailyHighlightMapper dailyHighlightMapper,
                                  OpenAiClient openAiClient,
                                  DailyHighlightProperties dailyHighlightProperties,
                                  BriefingProperties briefingProperties) {
        this.dailyHighlightMapper = dailyHighlightMapper;
        this.openAiClient = openAiClient;
        this.dailyHighlightProperties = dailyHighlightProperties;
        this.briefingProperties = briefingProperties;
    }

    // [무엇을 받아서] 입력값 없음(항상 "오늘" 기준).
    // [무엇을 하고] recompute(LocalDate.now())를 그대로 호출합니다. DailyHighlightScheduler가
    //              하루 3번 고정 시각에 부르는 진입점입니다.
    // [무엇을 돌려주는지] 실제로 계산해서 저장했으면 true, 재료가 부족해 건너뛰었으면 false
    //              — 스케줄러가 이 값으로 실행 결과를 로그에 남깁니다.
    public boolean recomputeForToday() {
        return recompute(LocalDate.now());
    }

    // [무엇을 받아서] 재계산할 날짜.
    // [무엇을 하고] BriefingMapper.selectTopBriefings와 정확히 같은 모집단(그 날짜
    //              기준 중요도 상위 briefing.top-n건)의 요약을 모읍니다 — "오늘의
    //              브리핑" 화면에 실제로 보이는 뉴스와 항상 같은 재료를 쓰기 위함입니다.
    //              건수가 daily-highlight.min-news-count 미만이면(여러 뉴스를 관통하는
    //              "흐름"이라는 개념 자체가 성립하기 어려운 건수라) LLM을 호출하지 않고
    //              조용히 끝냅니다 — 2건짜리 뉴스를 놓고 억지 공통점을 짜낼 위험을 코드
    //              레벨에서 차단하는 것이 핵심입니다. 충분하면 LLM을 불러 한 문장을 받고,
    //              daily_highlight에 그 날짜 행을 UPSERT합니다.
    // [무엇을 돌려주는지] 실제로 계산해서 저장했으면 true, 재료가 부족해 건너뛰었으면
    //              false — backfillMissing()이 이 값으로 진행 상황을 집계합니다.
    public boolean recompute(LocalDate date) {
        List<String> summaries = dailyHighlightMapper.selectSummariesForHighlight(
                date, briefingProperties.getTopN());

        if (summaries.size() < dailyHighlightProperties.getMinNewsCount()) {
            log.debug("Skipping daily highlight for {}: only {} news in top {} (min {})",
                    date, summaries.size(), briefingProperties.getTopN(),
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
