package com.jobnews.collector;

import com.jobnews.news.News;
import com.jobnews.news.NewsService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
        ArticleContentFetcher articleContentFetcher = mock(ArticleContentFetcher.class);
        NewsService newsService = mock(NewsService.class);
        News news = new News("https://example.com/a", "title", "desc", "테스트소스", null);
        when(rssFetcher.fetch(source)).thenReturn(List.of(news));
        when(newsService.exists(any())).thenReturn(false); // 신규 URL이라 크롤링 대상임
        when(articleContentFetcher.fetch(any(), any())).thenReturn("본문 전체 텍스트");
        when(newsService.saveIfNew(any())).thenReturn(true);

        NewsCollectorService collector = new NewsCollectorService(
                rssFetcher, articleContentFetcher, newsService, sourcesOf(source), retryProperties());

        collector.collect();

        verify(rssFetcher, times(1)).fetch(source);
        verify(articleContentFetcher, times(1)).fetch("https://example.com/a", "테스트소스");
        verify(newsService, times(1)).saveIfNew(news);
        assertThat(news.getDescription()).isEqualTo("본문 전체 텍스트");
    }

    @Test
    void retriesOnFailureAndSucceedsBeforeExhaustingAttempts() {
        RssFetcher rssFetcher = mock(RssFetcher.class);
        ArticleContentFetcher articleContentFetcher = mock(ArticleContentFetcher.class);
        NewsService newsService = mock(NewsService.class);
        News news = new News("https://example.com/a", "title", "desc", "테스트소스", null);
        when(rssFetcher.fetch(source))
                .thenThrow(new RssFetchException("boom", new RuntimeException()))
                .thenThrow(new RssFetchException("boom", new RuntimeException()))
                .thenReturn(List.of(news));
        when(newsService.exists(any())).thenReturn(false);

        NewsCollectorService collector = new NewsCollectorService(
                rssFetcher, articleContentFetcher, newsService, sourcesOf(source), retryProperties());

        collector.collect();

        verify(rssFetcher, times(3)).fetch(source);
        verify(newsService, times(1)).saveIfNew(news);
    }

    @Test
    void givesUpAfterMaxAttemptsAndSkipsSaving() {
        RssFetcher rssFetcher = mock(RssFetcher.class);
        ArticleContentFetcher articleContentFetcher = mock(ArticleContentFetcher.class);
        NewsService newsService = mock(NewsService.class);
        when(rssFetcher.fetch(source))
                .thenThrow(new RssFetchException("boom", new RuntimeException()));

        NewsCollectorService collector = new NewsCollectorService(
                rssFetcher, articleContentFetcher, newsService, sourcesOf(source), retryProperties());

        collector.collect();

        // 최초 시도 1회 + 재시도 3회 = 총 4회
        verify(rssFetcher, times(4)).fetch(source);
        verifyNoInteractions(newsService);
        verifyNoInteractions(articleContentFetcher);
    }

    @Test
    void skipsCrawlingAndSavingWhenUrlAlreadyExists() {
        RssFetcher rssFetcher = mock(RssFetcher.class);
        ArticleContentFetcher articleContentFetcher = mock(ArticleContentFetcher.class);
        NewsService newsService = mock(NewsService.class);
        News news = new News("https://example.com/a", "title", "desc", "테스트소스", null);
        when(rssFetcher.fetch(source)).thenReturn(List.of(news));
        when(newsService.exists("https://example.com/a")).thenReturn(true); // 이미 저장된 URL

        NewsCollectorService collector = new NewsCollectorService(
                rssFetcher, articleContentFetcher, newsService, sourcesOf(source), retryProperties());

        collector.collect();

        // 이미 있는 기사는 원문 크롤링 요청 자체를 보내면 안 된다 (불필요한 외부 요청 방지)
        verifyNoInteractions(articleContentFetcher);
        verify(newsService, never()).saveIfNew(any());
    }

    @Test
    void savesNewsEvenWhenArticleContentCrawlFails() {
        RssFetcher rssFetcher = mock(RssFetcher.class);
        ArticleContentFetcher articleContentFetcher = mock(ArticleContentFetcher.class);
        NewsService newsService = mock(NewsService.class);
        News news = new News("https://example.com/a", "title", "desc", "테스트소스", null);
        when(rssFetcher.fetch(source)).thenReturn(List.of(news));
        when(newsService.exists(any())).thenReturn(false);
        when(articleContentFetcher.fetch(any(), any()))
                .thenThrow(new ArticleFetchException("selector not found"));
        when(newsService.saveIfNew(any())).thenReturn(true);

        NewsCollectorService collector = new NewsCollectorService(
                rssFetcher, articleContentFetcher, newsService, sourcesOf(source), retryProperties());

        collector.collect();

        // 크롤링이 실패해도 RSS 기반 저장(title/url)은 그대로 진행되고,
        // description만 null로 남아야 한다.
        verify(newsService, times(1)).saveIfNew(news);
        assertThat(news.getDescription()).isNull();
    }
}
