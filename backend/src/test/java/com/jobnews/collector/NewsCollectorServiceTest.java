package com.jobnews.collector;

import com.jobnews.news.News;
import com.jobnews.news.NewsService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NewsCollectorServiceTest {

    private final RssSourceProperties.Source source = new RssSourceProperties.Source();

    private RssSourceProperties sourcesOf(RssSourceProperties.Source... sources) {
        RssSourceProperties props = new RssSourceProperties();
        props.setSources(List.of(sources));
        return props;
    }

    private CollectorRetryProperties retryProperties() {
        CollectorRetryProperties props = new CollectorRetryProperties();
        props.setMaxAttempts(3);
        props.setBackoffSeconds(List.of(0, 0, 0)); // 테스트에서는 대기 없이 즉시 재시도
        return props;
    }

    {
        source.setName("테스트소스");
        source.setUrl("https://example.com/rss");
    }

    @Test
    void savesAllFetchedNewsWhenFirstAttemptSucceeds() {
        RssFetcher rssFetcher = mock(RssFetcher.class);
        NewsService newsService = mock(NewsService.class);
        News news = new News("https://example.com/a", "title", "desc", "테스트소스", null);
        when(rssFetcher.fetch(source)).thenReturn(List.of(news));
        when(newsService.saveIfNew(any())).thenReturn(true);

        NewsCollectorService collector = new NewsCollectorService(
                rssFetcher, newsService, sourcesOf(source), retryProperties());

        collector.collect();

        verify(rssFetcher, times(1)).fetch(source);
        verify(newsService, times(1)).saveIfNew(news);
    }

    @Test
    void retriesOnFailureAndSucceedsBeforeExhaustingAttempts() {
        RssFetcher rssFetcher = mock(RssFetcher.class);
        NewsService newsService = mock(NewsService.class);
        News news = new News("https://example.com/a", "title", "desc", "테스트소스", null);
        when(rssFetcher.fetch(source))
                .thenThrow(new RssFetchException("boom", new RuntimeException()))
                .thenThrow(new RssFetchException("boom", new RuntimeException()))
                .thenReturn(List.of(news));

        NewsCollectorService collector = new NewsCollectorService(
                rssFetcher, newsService, sourcesOf(source), retryProperties());

        collector.collect();

        verify(rssFetcher, times(3)).fetch(source);
        verify(newsService, times(1)).saveIfNew(news);
    }

    @Test
    void givesUpAfterMaxAttemptsAndSkipsSaving() {
        RssFetcher rssFetcher = mock(RssFetcher.class);
        NewsService newsService = mock(NewsService.class);
        when(rssFetcher.fetch(source))
                .thenThrow(new RssFetchException("boom", new RuntimeException()));

        NewsCollectorService collector = new NewsCollectorService(
                rssFetcher, newsService, sourcesOf(source), retryProperties());

        collector.collect();

        // 최초 시도 1회 + 재시도 3회 = 총 4회
        verify(rssFetcher, times(4)).fetch(source);
        verifyNoInteractions(newsService);
    }
}
