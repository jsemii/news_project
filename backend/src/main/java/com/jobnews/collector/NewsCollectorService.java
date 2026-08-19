package com.jobnews.collector;

import com.jobnews.news.News;
import com.jobnews.news.NewsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NewsCollectorService {

    private static final Logger log = LoggerFactory.getLogger(NewsCollectorService.class);

    private final RssFetcher rssFetcher;
    private final NewsService newsService;
    private final RssSourceProperties rssSourceProperties;
    private final CollectorRetryProperties retryProperties;

    public NewsCollectorService(RssFetcher rssFetcher,
                                 NewsService newsService,
                                 RssSourceProperties rssSourceProperties,
                                 CollectorRetryProperties retryProperties) {
        this.rssFetcher = rssFetcher;
        this.newsService = newsService;
        this.rssSourceProperties = rssSourceProperties;
        this.retryProperties = retryProperties;
    }

    public void collect() {
        for (RssSourceProperties.Source source : rssSourceProperties.getSources()) {
            collectSource(source);
        }
    }

    private void collectSource(RssSourceProperties.Source source) {
        List<News> fetched = fetchWithRetry(source);
        if (fetched == null) {
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

    private List<News> fetchWithRetry(RssSourceProperties.Source source) {
        RssFetchException lastFailure;
        try {
            return rssFetcher.fetch(source);
        } catch (RssFetchException e) {
            lastFailure = e;
            log.warn("[{}] fetch failed, will retry: {}", source.getName(), e.getMessage());
        }

        int maxAttempts = retryProperties.getMaxAttempts();
        List<Integer> backoffSeconds = retryProperties.getBackoffSeconds();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            sleep(backoffSecondsFor(backoffSeconds, attempt));
            try {
                List<News> result = rssFetcher.fetch(source);
                log.info("[{}] retry #{} succeeded", source.getName(), attempt);
                return result;
            } catch (RssFetchException e) {
                lastFailure = e;
                log.warn("[{}] retry #{} failed: {}", source.getName(), attempt, e.getMessage());
            }
        }

        log.error("[{}] gave up after {} retries", source.getName(), maxAttempts, lastFailure);
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
            throw new RssFetchException("Retry wait interrupted", e);
        }
    }
}
