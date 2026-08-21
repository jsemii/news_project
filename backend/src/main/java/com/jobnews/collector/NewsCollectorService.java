package com.jobnews.collector;

import com.jobnews.news.News;
import com.jobnews.news.NewsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * [전체 흐름에서의 위치] "수집" 단계 전체를 지휘하는 "오케스트레이터(여러 부품을
 * 순서대로 지휘하는 역할)" 클래스입니다. RssSourceProperties(어떤 소스를 볼지),
 * RssFetcher(RSS 목록 가져오고 파싱하기), CollectorRetryProperties(RSS 실패 시 몇 번,
 * 몇 초씩 재시도할지), NewsService(중복 체크 후 저장) — 이 부품들을 순서대로 조합해서,
 * "정해진 모든 RSS 소스를 하나씩 돌면서, RSS 목록을 가져와 신규 기사만 저장한다"는
 * 흐름을 완성합니다. 실제 실행 트리거는 NewsScheduler가 담당합니다.
 * ⚠️ 예전에는 여기서 기사 원문까지 크롤링해서 저장했었는데, 저작권 리스크 때문에
 * 원문 크롤링은 AI 구조화 단계(ai 패키지)로 옮겨서 "크롤링 즉시 LLM에 넣고 버림"
 * 방식으로 바뀌었습니다(docs/troubleshooting.md 참고). 이 클래스는 이제 RSS
 * 메타데이터(제목/링크/발행일)만 다룹니다.
 */
// @Service: 이 클래스가 비즈니스 로직(단순 CRUD가 아니라 "재시도한다", "중복이면
// 건너뛴다" 같은 업무 규칙)을 담당하는 서비스 계층임을 표시하고, 스프링이 객체를
// 만들어 관리하게 합니다. 안 쓰면 NewsScheduler가 이 클래스를 자동으로 주입받지 못합니다.
@Service
public class NewsCollectorService {

    private static final Logger log = LoggerFactory.getLogger(NewsCollectorService.class);

    private final RssFetcher rssFetcher;
    private final NewsService newsService;
    private final RssSourceProperties rssSourceProperties;
    private final CollectorRetryProperties retryProperties;

    // [무엇을 받아서] 이 클래스가 지휘할 4개의 부품(RssFetcher, NewsService,
    //              RssSourceProperties, CollectorRetryProperties)을 스프링이
    //              자동으로 만들어서 넘겨줍니다(생성자 주입).
    // [무엇을 하고 돌려주는지] 받은 것들을 필드에 저장해서 아래 메서드들에서 사용합니다.
    public NewsCollectorService(RssFetcher rssFetcher,
                                 NewsService newsService,
                                 RssSourceProperties rssSourceProperties,
                                 CollectorRetryProperties retryProperties) {
        this.rssFetcher = rssFetcher;
        this.newsService = newsService;
        this.rssSourceProperties = rssSourceProperties;
        this.retryProperties = retryProperties;
    }

    /**
     * [무엇을 받아서] 입력값이 없습니다(설정된 모든 소스를 대상으로 합니다).
     * [무엇을 하고] application.yml에 등록된 RSS 소스(현재 전자신문, 연합뉴스)를
     *              한 개씩 순서대로 collectSource()에 넘겨서 처리합니다.
     * [무엇을 돌려주는지] 반환값 없음(void). 결과는 로그와 DB 저장으로 확인합니다.
     * [왜 필요한지] NewsScheduler가 "지금 전체 수집을 한 번 실행해줘"라고 부를 때
     *              바깥에서 부르는 진입점(entry point) 메서드가 필요하기 때문입니다.
     */
    public void collect() {
        for (RssSourceProperties.Source source : rssSourceProperties.getSources()) {
            collectSource(source);
        }
    }

    // [무엇을 받아서] RSS 소스 하나(예: 전자신문)를 받습니다.
    // [무엇을 하고] 1) fetchWithRetry()로 재시도까지 포함해서 RSS 기사 목록(제목/링크/발행일)을
    //                 가져오고,
    //              2) 완전히 실패했다면(fetched == null) 이 소스는 포기하고 조용히 끝냅니다
    //                 (다음 소스 처리에는 영향을 주지 않습니다 — 한 소스가 실패해도
    //                 전체 수집이 멈추지 않게 하려는 의도입니다),
    //              3) 성공했다면 기사를 하나씩 NewsService.saveIfNew()에 넘겨 "URL 중복
    //                 제거" 규칙에 따라 신규 기사만 저장합니다.
    // [무엇을 돌려주는지] 반환값 없음. 대신 로그로 "몇 건 가져왔고 몇 건 새로 저장했는지" 남깁니다.
    private void collectSource(RssSourceProperties.Source source) {
        List<News> fetched = fetchWithRetry(source);
        if (fetched == null) {
            // fetchWithRetry가 null을 돌려준다는 것은 "최초 시도 + 재시도까지 전부 실패했다"는
            // 뜻입니다. 이 시점에 이미 fetchWithRetry 안에서 ERROR 로그를 남겼으므로,
            // 여기서는 그냥 이 소스 처리를 중단하고 다음 소스로 넘어갑니다.
            return;
        }

        int savedCount = 0;
        for (News news : fetched) {
            if (newsService.saveIfNew(news)) {
                savedCount++;
            }
        }
        log.info("[{}] fetched {} items, saved {} new", source.getName(), fetched.size(), savedCount);
    }

    /**
     * [무엇을 받아서] RSS 소스 하나를 받습니다.
     * [무엇을 하고] "최초 시도 1회 + 실패 시 재시도 최대 N회(N=collector.retry.max-attempts)"
     *              전략으로 RssFetcher.fetch()를 반복 호출합니다. 재시도 사이에는
     *              backoffSecondsFor()가 계산한 시간만큼 대기합니다(2초→4초→8초처럼
     *              점점 늘어나는 지수 백오프). 재시도 도중 한 번이라도 성공하면 그 결과를
     *              즉시 돌려주고 더 이상 재시도하지 않습니다.
     * [무엇을 돌려주는지] 성공하면 가져온 기사 목록. 최초 시도 + 재시도를 전부 소진했는데도
     *              계속 실패하면 null을 돌려주고, "재시도를 다 썼는데도 실패했다"는
     *              사실을 ERROR 레벨 로그로 남깁니다(요구사항: "재시도도 실패하면 로그를 남긴다").
     * [try/catch 의도] RssFetcher.fetch()가 던지는 RssFetchException(네트워크 오류,
     *              XML 파싱 실패 등 모든 실패를 대표하는 예외)을 여기서 잡아서, 예외가
     *              메서드 밖으로 전파되어 프로그램이 죽는 대신, "실패를 기록하고 다음
     *              시도로 넘어간다"는 재시도 로직으로 흡수시킵니다.
     */
    private List<News> fetchWithRetry(RssSourceProperties.Source source) {
        RssFetchException lastFailure;
        try {
            // 가장 먼저 한 번은 대기 없이 바로 시도합니다(정상적인 경우 대부분 여기서 성공).
            return rssFetcher.fetch(source);
        } catch (RssFetchException e) {
            lastFailure = e;
            log.warn("[{}] fetch failed, will retry: {}", source.getName(), e.getMessage());
        }

        int maxAttempts = retryProperties.getMaxAttempts();
        List<Integer> backoffSeconds = retryProperties.getBackoffSeconds();

        // for: 최초 시도가 실패했을 때만 이 반복문에 들어옵니다. attempt는 1부터
        // maxAttempts(기본 3)까지 증가하며, 각 반복이 "N번째 재시도"에 해당합니다.
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            sleep(backoffSecondsFor(backoffSeconds, attempt));
            try {
                List<News> result = rssFetcher.fetch(source);
                log.info("[{}] retry #{} succeeded", source.getName(), attempt);
                // 재시도 중 성공하면 남은 재시도 횟수를 쓰지 않고 바로 결과를 돌려주고 끝냅니다.
                return result;
            } catch (RssFetchException e) {
                lastFailure = e;
                log.warn("[{}] retry #{} failed: {}", source.getName(), attempt, e.getMessage());
            }
        }

        // 여기까지 왔다는 것은 최초 시도 + 재시도(maxAttempts회)가 전부 실패했다는 뜻입니다.
        // 요구사항대로 ERROR 레벨로 최종 실패를 기록하고, 마지막 실패 원인(lastFailure)도
        // 함께 남겨서 나중에 로그만 보고도 원인을 추적할 수 있게 합니다.
        log.error("[{}] gave up after {} retries", source.getName(), maxAttempts, lastFailure);
        return null;
    }

    // [무엇을 받아서] 설정된 대기 시간 목록(예: [2, 4, 8])과 현재 몇 번째 재시도인지(attempt)를 받습니다.
    // [무엇을 하고] attempt번째 재시도에 해당하는 대기 시간을 목록에서 찾습니다. 만약
    //              attempt가 목록 길이보다 크면(예: 재시도를 4번 이상 설정한 경우) 마지막
    //              값을 계속 재사용합니다 — 설정값이 부족해도 예외 없이 안전하게 동작하도록
    //              하기 위한 방어적인 처리입니다.
    // [무엇을 돌려주는지] 대기할 초(seconds) 값.
    private long backoffSecondsFor(List<Integer> backoffSeconds, int attempt) {
        if (backoffSeconds.isEmpty()) {
            return 0;
        }
        int index = Math.min(attempt - 1, backoffSeconds.size() - 1);
        return backoffSeconds.get(index);
    }

    // [무엇을 받아서] 대기할 시간(초)을 받습니다.
    // [무엇을 하고] 그만큼 현재 스레드(작업 흐름)를 잠재웁니다(Thread.sleep). 이 서비스는
    //              스케줄러가 별도의 백그라운드 스레드에서 실행하므로, 여기서 잠깐 멈춰도
    //              웹 요청을 처리하는 다른 스레드에는 영향이 없습니다.
    // [try/catch 의도] Thread.sleep은 다른 스레드가 이 대기를 강제로 깨울 때(interrupt)
    //              InterruptedException을 던지도록 자바 언어가 강제합니다. 이 프로젝트는
    //              그런 상황을 정상적으로 처리할 필요가 없으므로, 이를 RssFetchException으로
    //              바꿔 던져서 상위 로직이 "재시도 실패"로 취급하게 만듭니다. 또한
    //              Thread.currentThread().interrupt()로 "중단 신호"를 다시 세워두는 것은
    //              자바의 표준 관례입니다(신호를 무시하지 않고 다음 코드에 전달).
    private void sleep(long seconds) {
        if (seconds <= 0) {
            return;
        }
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RssFetchException("Retry wait interrupted", e);
        }
    }
}
