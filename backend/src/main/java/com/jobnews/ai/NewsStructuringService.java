package com.jobnews.ai;

import com.jobnews.collector.ArticleContentFetcher;
import com.jobnews.collector.ArticleFetchException;
import com.jobnews.news.News;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * [전체 흐름에서의 위치] "AI 뉴스 구조화" 단계 전체를 지휘하는 오케스트레이터입니다.
 * 아직 분석 안 된 뉴스를 찾아서(NewsAnalysisMapper), 하나씩:
 *   1) 제목으로 먼저 걸러내고(크롤링 전, 공짜),
 *   2) 원문을 크롤링하고(ArticleContentFetcher — collector 패키지의 클래스를 가져다 씁니다.
 *      "수집"이 아니라 "분석 직전"에 크롤링하는 이유는, 크롤링한 원문을 곧바로 LLM에
 *      넘기고 이 메서드가 끝나면 버리기 위함입니다 — 어디에도 저장하지 않습니다),
 *   3) 원문이 너무 짧거나 없으면 걸러내고,
 *   4) 1단계 LLM 호출(원문 → 일반 요약)과 2단계 LLM 호출(요약 → 직무별 재해석)을
 *      순서대로 실행하고,
 *   5) 성공하면 저장(NewsAnalysisSaver)합니다.
 * 실제 실행 트리거는 NewsStructuringScheduler(자동, cron)와 StructuringController(수동)가 담당합니다.
 */
@Service
public class NewsStructuringService {

    private static final Logger log = LoggerFactory.getLogger(NewsStructuringService.class);

    private final ArticleContentFetcher articleContentFetcher;
    private final OpenAiClient openAiClient;
    private final NewsAnalysisSaver newsAnalysisSaver;
    private final NewsAnalysisMapper newsAnalysisMapper;
    private final OpenAiProperties openAiProperties;
    private final NewsRelevanceFilter newsRelevanceFilter;
    private final DailyHighlightService dailyHighlightService;

    public NewsStructuringService(ArticleContentFetcher articleContentFetcher,
                                   OpenAiClient openAiClient,
                                   NewsAnalysisSaver newsAnalysisSaver,
                                   NewsAnalysisMapper newsAnalysisMapper,
                                   OpenAiProperties openAiProperties,
                                   NewsRelevanceFilter newsRelevanceFilter,
                                   DailyHighlightService dailyHighlightService) {
        this.articleContentFetcher = articleContentFetcher;
        this.openAiClient = openAiClient;
        this.newsAnalysisSaver = newsAnalysisSaver;
        this.newsAnalysisMapper = newsAnalysisMapper;
        this.openAiProperties = openAiProperties;
        this.newsRelevanceFilter = newsRelevanceFilter;
        this.dailyHighlightService = dailyHighlightService;
    }

    /**
     * [무엇을 받아서] 입력값 없음(설정된 배치 크기만큼만 처리함).
     * [무엇을 하고] 미분석 뉴스 중 오래된 것부터 openai.batch-size(기본 20)건만 조회해서
     *              하나씩 순서대로 구조화합니다.
     * [무엇을 돌려주는지] 이번 배치 처리 결과 요약(StructuringSummary).
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

        if (succeeded > 0) {
            recomputeDailyHighlight();
        }

        int remainingBacklog = newsAnalysisMapper.countUnanalyzedNews();
        return new StructuringSummary(unanalyzed.size(), filteredOut, succeeded, failed, remainingBacklog);
    }

    // [무엇을 받아서] 입력값 없음.
    // [무엇을 하고] DailyHighlightService.recomputeForToday()를 호출합니다("오늘 한 줄
    //              요약"을 이번 배치에서 새로 분석된 내용까지 반영해 다시 계산). try/catch로
    //              감싸는 이유: 이 기능은 부가 기능이라, 여기서 실패(예: OpenAI 일시적
    //              오류)하더라도 방금 정상적으로 끝난 뉴스 구조화 배치 결과 자체를 실패로
    //              만들면 안 되기 때문입니다 — 로그만 남기고 넘어갑니다.
    private void recomputeDailyHighlight() {
        try {
            dailyHighlightService.recomputeForToday();
        } catch (Exception e) {
            log.warn("Failed to recompute daily highlight, skipping this cycle", e);
        }
    }

    private enum StructureOutcome {
        FILTERED, SUCCEEDED, FAILED
    }

    // [무엇을 받아서] 아직 분석되지 않은 뉴스 하나를 받습니다.
    // [무엇을 하고]
    //   1) 수집된 지 너무 오래되지 않았는지 먼저 봅니다 — 걸리면 크롤링도 안 하고 바로
    //      제외합니다(가장 저렴한 판단이라 제일 먼저 확인).
    //   2) 제목 키워드 필터를 봅니다 — 걸리면 마찬가지로 크롤링 없이 바로 제외합니다.
    //   3) 원문을 크롤링합니다(이 메서드의 지역 변수 rawText에만 존재 — DB에 저장 안 함).
    //   4) 크롤링 실패했거나 내용이 너무 짧으면 제외합니다(둘 다 "AI에게 넘길 만한
    //      내용이 없다"는 같은 결론이라 같은 처리).
    //   5) 1단계(일반 요약) 호출 → 실패하면 이 뉴스는 FAILED로 끝냅니다.
    //   6) 2단계(직무별 재해석) 호출 → 실패하면 1단계 결과까지 통째로 버리고 FAILED로
    //      끝냅니다(반쪽 데이터를 안 남기기 위함 — NewsAnalysisSaver 주석 참고).
    //   7) 둘 다 성공하면 저장합니다.
    // [무엇을 돌려주는지] 이 뉴스가 필터링/성공/실패 중 무엇이었는지.
    private StructureOutcome structureOne(News news) {
        if (newsRelevanceFilter.isTooOld(news)) {
            markFilteredOut(news, FilterReason.TOO_OLD);
            return StructureOutcome.FILTERED;
        }

        if (newsRelevanceFilter.isTitleExcluded(news)) {
            markFilteredOut(news, FilterReason.TITLE_EXCLUDED);
            return StructureOutcome.FILTERED;
        }

        String rawText = tryCrawl(news);
        if (newsRelevanceFilter.isContentTooShort(rawText)) {
            markFilteredOut(news, FilterReason.CONTENT_TOO_SHORT);
            return StructureOutcome.FILTERED;
        }

        GeneralAnalysisResult general = analyzeGeneralWithRetry(news, rawText);
        if (general == null) {
            return StructureOutcome.FAILED;
        }

        List<JobAnalysisResult> jobs = analyzeJobsWithRetry(news, general.getSummary());
        if (jobs == null) {
            log.warn("[newsId={}] stage 2 failed after retries, discarding stage 1 result too", news.getId());
            return StructureOutcome.FAILED;
        }

        // try/catch 의도: 이 배치와 "동시에" 다른 요청(예: 사람이 Swagger UI에서 수동
        // 트리거를 직접 누르는 경우, 또는 스케줄러와 겹치는 경우)이 같은 뉴스를 먼저
        // 분석해서 이미 저장해버렸을 수 있습니다. 그러면 news_analysis.news_id의 UNIQUE
        // 제약에 걸려 DuplicateKeyException이 납니다. 예전에는 이 예외가 그대로 튀어나가
        // HTTP 요청 전체가 500 에러로 죽고, 같은 배치의 나머지 뉴스들까지 처리되지 못하고
        // 버려졌습니다(실제로 겪은 문제 — docs/troubleshooting.md 참고). 이제는 "누군가
        // 이미 분석했다"는 뜻으로 받아들여 조용히 넘어가고, 배치의 나머지는 계속 처리합니다.
        try {
            newsAnalysisSaver.save(news.getId(), general, jobs);
        } catch (DuplicateKeyException e) {
            log.warn("[newsId={}] already analyzed by a concurrent request, skipping", news.getId());
            return StructureOutcome.SUCCEEDED;
        }
        log.info("News structured: newsId={}", news.getId());
        return StructureOutcome.SUCCEEDED;
    }

    // insertFilteredOut으로 "이 뉴스는 검토했고 제외하기로 했다"는 표시를 남깁니다. 이
    // 표시가 없으면 다음 배치에서 selectUnanalyzedNews가 이 뉴스를 다시 가져와 똑같은
    // 판단을 영원히 반복하게 됩니다(실제로 겪은 문제 — docs/troubleshooting.md 참고).
    // 크롤링 실패도 이 표시를 남기는 이유: ArticleContentFetcher는 재시도를 안 하기로
    // 했고(사이트 구조 변경 등 영구적인 실패일 가능성이 높음), 재시도해도 똑같이 실패할
    // 가능성이 높은 걸 매 배치 다시 시도하는 것보다는 한 번 포기하는 편이 낫다고 판단했습니다.
    // reason을 FilterReason(자유 문장이 아니라 정해진 값)으로 받는 이유는 news_filtered_out.reason에
    // 그대로 저장되기 때문입니다 — 나중에 "얼마나 많은 뉴스가 어떤 이유로 걸러졌는지"
    // 집계할 때 값이 표준화돼 있어야 의미가 있습니다.
    private void markFilteredOut(News news, FilterReason reason) {
        newsAnalysisMapper.insertFilteredOut(news.getId(), reason.name());
        log.debug("[newsId={}] filtered out before/after crawl ({})", news.getId(), reason);
    }

    // [무엇을 받아서] 뉴스(로그용)와 크롤링할 url/소스를 담은 뉴스 객체를 받습니다.
    // [무엇을 하고] ArticleContentFetcher로 원문 크롤링을 "재시도 없이 1회만" 시도합니다
    //              (수집 단계에서 쓰던 것과 같은 정책 — 언론사 서버 부담을 줄이기 위함).
    // [무엇을 돌려주는지] 성공하면 원문 텍스트. 실패하면 null.
    private String tryCrawl(News news) {
        try {
            return articleContentFetcher.fetch(news.getUrl(), news.getSource());
        } catch (ArticleFetchException e) {
            log.warn("[newsId={}] article content crawl failed: {}", news.getId(), e.getMessage());
            return null;
        }
    }

    // [무엇을 받아서] 뉴스(로그/제목용)와 크롤링한 원문을 받습니다.
    // [무엇을 하고] "최초 시도 1회 + 실패 시 재시도 최대 N회" 전략으로
    //              OpenAiClient.analyzeGeneral()을 반복 호출합니다.
    // [무엇을 돌려주는지] 성공하면 1단계 결과. 전부 실패하면 null.
    private GeneralAnalysisResult analyzeGeneralWithRetry(News news, String rawText) {
        AiStructureException lastFailure;
        try {
            return openAiClient.analyzeGeneral(news.getTitle(), rawText);
        } catch (AiStructureException e) {
            lastFailure = e;
            log.warn("[newsId={}] stage 1(general) failed, will retry: {}", news.getId(), e.getMessage());
        }

        int maxAttempts = openAiProperties.getRetry().getMaxAttempts();
        List<Integer> backoffSeconds = openAiProperties.getRetry().getBackoffSeconds();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            sleep(backoffSecondsFor(backoffSeconds, attempt));
            try {
                GeneralAnalysisResult result = openAiClient.analyzeGeneral(news.getTitle(), rawText);
                log.info("[newsId={}] stage 1(general) retry #{} succeeded", news.getId(), attempt);
                return result;
            } catch (AiStructureException e) {
                lastFailure = e;
                log.warn("[newsId={}] stage 1(general) retry #{} failed: {}", news.getId(), attempt, e.getMessage());
            }
        }

        log.error("[newsId={}] stage 1(general) gave up after {} retries", news.getId(), maxAttempts, lastFailure);
        return null;
    }

    // [무엇을 받아서] 뉴스(로그용)와 1단계가 만든 일반 요약을 받습니다.
    // [무엇을 하고] 위와 같은 재시도 전략으로 OpenAiClient.analyzeJobs()를 반복 호출합니다.
    // [무엇을 돌려주는지] 성공하면 직무별 결과 목록(3개). 전부 실패하면 null.
    private List<JobAnalysisResult> analyzeJobsWithRetry(News news, String generalSummary) {
        AiStructureException lastFailure;
        try {
            return openAiClient.analyzeJobs(generalSummary);
        } catch (AiStructureException e) {
            lastFailure = e;
            log.warn("[newsId={}] stage 2(jobs) failed, will retry: {}", news.getId(), e.getMessage());
        }

        int maxAttempts = openAiProperties.getRetry().getMaxAttempts();
        List<Integer> backoffSeconds = openAiProperties.getRetry().getBackoffSeconds();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            sleep(backoffSecondsFor(backoffSeconds, attempt));
            try {
                List<JobAnalysisResult> result = openAiClient.analyzeJobs(generalSummary);
                log.info("[newsId={}] stage 2(jobs) retry #{} succeeded", news.getId(), attempt);
                return result;
            } catch (AiStructureException e) {
                lastFailure = e;
                log.warn("[newsId={}] stage 2(jobs) retry #{} failed: {}", news.getId(), attempt, e.getMessage());
            }
        }

        log.error("[newsId={}] stage 2(jobs) gave up after {} retries", news.getId(), maxAttempts, lastFailure);
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
