package com.jobnews.ai;

import com.jobnews.news.News;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * [전체 흐름에서의 위치] "AI 뉴스 구조화" 단계 전체를 지휘하는 오케스트레이터입니다.
 * collector 패키지의 NewsCollectorService와 같은 역할을, "수집"이 아니라 "AI 분석"에
 * 대해 담당합니다: 아직 분석 안 된 뉴스를 찾아서(NewsAnalysisMapper), 하나씩 OpenAI에
 * 보내고(OpenAiClient), 실패하면 재시도하고, 성공하면 저장(NewsAnalysisSaver)합니다.
 * 실제 실행 트리거는 Phase 6의 NewsStructuringScheduler가 담당합니다.
 */
@Service
public class NewsStructuringService {

    private static final Logger log = LoggerFactory.getLogger(NewsStructuringService.class);

    private final OpenAiClient openAiClient;
    private final NewsAnalysisSaver newsAnalysisSaver;
    private final NewsAnalysisMapper newsAnalysisMapper;
    private final OpenAiProperties openAiProperties;
    private final NewsRelevanceFilter newsRelevanceFilter;

    public NewsStructuringService(OpenAiClient openAiClient,
                                   NewsAnalysisSaver newsAnalysisSaver,
                                   NewsAnalysisMapper newsAnalysisMapper,
                                   OpenAiProperties openAiProperties,
                                   NewsRelevanceFilter newsRelevanceFilter) {
        this.openAiClient = openAiClient;
        this.newsAnalysisSaver = newsAnalysisSaver;
        this.newsAnalysisMapper = newsAnalysisMapper;
        this.openAiProperties = openAiProperties;
        this.newsRelevanceFilter = newsRelevanceFilter;
    }

    /**
     * [무엇을 받아서] 입력값 없음(설정된 배치 크기만큼만 처리함).
     * [무엇을 하고] 미분석 뉴스 중 오래된 것부터 openai.batch-size(기본 20)건만 조회해서
     *              하나씩 순서대로 구조화합니다. 미분석 뉴스가 배치 크기보다 많으면 나머지는
     *              이번 호출에서 처리되지 않고 다음 실행(스케줄 또는 재호출) 때 처리됩니다 —
     *              한 번의 호출이 미분석 뉴스를 전부 처리하려다 너무 오래 걸리거나 OpenAI
     *              비용이 한 번에 몰리는 것을 막기 위함입니다.
     * [무엇을 돌려주는지] 이번 배치에서 몇 건을 발견했고, 몇 건이 필터링됐고, 몇 건이
     *              성공/실패했는지, 그리고 아직 몇 건이 더 남았는지 담은 요약
     *              (StructuringSummary). 스케줄러는 이 값을 로그로만 남기고, 수동 트리거
     *              API는 이 값을 그대로 HTTP 응답으로 돌려줍니다.
     */
    public StructuringSummary structureAll() {
        List<News> unanalyzed = newsAnalysisMapper.selectUnanalyzedNews(openAiProperties.getBatchSize());
        log.info("Found {} unanalyzed news in this batch", unanalyzed.size());

        int filteredOut = 0;
        int succeeded = 0;
        int failed = 0;

        for (News news : unanalyzed) {
            StructureOutcome outcome = structureOne(news);
            switch (outcome) {
                case FILTERED -> filteredOut++;
                case SUCCEEDED -> succeeded++;
                case FAILED -> failed++;
            }
        }

        int remainingBacklog = newsAnalysisMapper.countUnanalyzedNews();
        return new StructuringSummary(unanalyzed.size(), filteredOut, succeeded, failed, remainingBacklog);
    }

    // 뉴스 한 건을 처리한 결과가 셋 중 무엇이었는지를 나타냅니다. structureAll()이 이 값을
    // 세어서 StructuringSummary(몇 건 성공/실패/필터링됐는지)를 만드는 데 사용합니다.
    private enum StructureOutcome {
        FILTERED, SUCCEEDED, FAILED
    }

    // [무엇을 받아서] 아직 분석되지 않은 뉴스 하나를 받습니다.
    // [무엇을 하고] 1) NewsRelevanceFilter로 "AI 구조화할 가치가 있는 뉴스인지" 먼저
    //              확인합니다. 가치가 낮다고 판단되면(예: "[속보] 코스피 급락" 같은 짧은
    //              시황 뉴스) OpenAI를 아예 호출하지 않고 건너뜁니다 — LLM 호출 비용을
    //              아끼기 위한 무료 사전 검사입니다.
    //              2) 가치가 있다면 analyzeWithRetry()로 재시도까지 포함해서 OpenAI 분석
    //              결과를 받고, 완전히 실패했다면(result == null) 이 뉴스는 포기하고
    //              다음 뉴스로 넘어갑니다(한 뉴스의 분석 실패가 전체 구조화 작업을 막지
    //              않도록 하려는 의도). 성공했다면 NewsAnalysisSaver로 저장합니다.
    // [무엇을 돌려주는지] 이 뉴스가 필터링/성공/실패 중 무엇이었는지.
    private StructureOutcome structureOne(News news) {
        if (!newsRelevanceFilter.isWorthAnalyzing(news)) {
            // insertFilteredOut으로 "이 뉴스는 검토했고 제외하기로 했다"는 표시를 남깁니다.
            // 이 표시가 없으면 다음 배치에서 selectUnanalyzedNews가 이 뉴스를 다시 가져와
            // 똑같이 필터링하는 과정을 영원히 반복하게 됩니다(실제로 겪은 문제).
            newsAnalysisMapper.insertFilteredOut(news.getId());
            log.debug("[newsId={}] filtered out before AI call (low relevance)", news.getId());
            return StructureOutcome.FILTERED;
        }

        AiAnalysisResult result = analyzeWithRetry(news);
        if (result == null) {
            return StructureOutcome.FAILED;
        }
        newsAnalysisSaver.save(news.getId(), result);
        log.info("News structured: newsId={}", news.getId());
        return StructureOutcome.SUCCEEDED;
    }

    /**
     * [무엇을 받아서] 분석할 뉴스를 받습니다.
     * [무엇을 하고] "최초 시도 1회 + 실패 시 재시도 최대 N회" 전략으로 OpenAiClient.analyze()를
     *              반복 호출합니다. NewsCollectorService.fetchWithRetry()와 완전히 같은
     *              구조이며, 설정값만 collector.retry 대신 openai.retry를 사용합니다.
     * [무엇을 돌려주는지] 성공하면 분석 결과. 최초 시도 + 재시도를 전부 소진하고도 실패하면
     *              null을 돌려주고 ERROR 로그를 남깁니다.
     */
    private AiAnalysisResult analyzeWithRetry(News news) {
        AiStructureException lastFailure;
        try {
            return openAiClient.analyze(news);
        } catch (AiStructureException e) {
            lastFailure = e;
            log.warn("[newsId={}] AI analysis failed, will retry: {}", news.getId(), e.getMessage());
        }

        int maxAttempts = openAiProperties.getRetry().getMaxAttempts();
        List<Integer> backoffSeconds = openAiProperties.getRetry().getBackoffSeconds();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            sleep(backoffSecondsFor(backoffSeconds, attempt));
            try {
                AiAnalysisResult result = openAiClient.analyze(news);
                log.info("[newsId={}] retry #{} succeeded", news.getId(), attempt);
                return result;
            } catch (AiStructureException e) {
                lastFailure = e;
                log.warn("[newsId={}] retry #{} failed: {}", news.getId(), attempt, e.getMessage());
            }
        }

        log.error("[newsId={}] gave up after {} retries", news.getId(), maxAttempts, lastFailure);
        return null;
    }

    private long backoffSecondsFor(List<Integer> backoffSeconds, int attempt) {
        if (backoffSeconds.isEmpty()) {
            return 0;
        }
        int index = Math.min(attempt - 1, backoffSeconds.size() - 1);
        return backoffSeconds.get(index);
    }

    private void sleep(long seconds) {
        if (seconds <= 0) {
            return;
        }
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiStructureException("Retry wait interrupted", e);
        }
    }
}
