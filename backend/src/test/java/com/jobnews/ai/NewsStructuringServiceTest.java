package com.jobnews.ai;

import com.jobnews.collector.ArticleContentFetcher;
import com.jobnews.collector.ArticleFetchException;
import com.jobnews.news.News;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class NewsStructuringServiceTest {

    private OpenAiProperties propertiesWith(int batchSize) {
        OpenAiProperties props = new OpenAiProperties();
        props.setBatchSize(batchSize);
        OpenAiProperties.Retry retry = new OpenAiProperties.Retry();
        retry.setMaxAttempts(3);
        retry.setBackoffSeconds(List.of(0, 0, 0)); // 테스트에서는 대기 없이 즉시 재시도
        props.setRetry(retry);
        return props;
    }

    // 아무것도 걸러내지 않는 "통과 전용" 필터입니다. 필터 자체의 동작은
    // NewsRelevanceFilterTest에서 검증하므로, 여기서는 재시도/저장 흐름에만 집중합니다.
    private NewsRelevanceFilter permissiveFilter() {
        NewsFilterProperties props = new NewsFilterProperties();
        props.setExcludeTitleKeywords(List.of());
        props.setMinContentLength(0);
        return new NewsRelevanceFilter(props);
    }

    private News newsWithId(long id) {
        News news = new News();
        news.setId(id);
        news.setTitle("테스트 뉴스");
        news.setUrl("https://example.com/news/" + id);
        news.setSource("테스트소스");
        return news;
    }

    private GeneralAnalysisResult sampleGeneral() {
        return new GeneralAnalysisResult("일반 요약", 7, "테스트 이유", List.of("금융"));
    }

    private List<JobAnalysisResult> sampleJobs() {
        return List.of(
                new JobAnalysisResult("IT전산", "이유1", "스킬1"),
                new JobAnalysisResult("데이터분석", "이유2", "스킬2"),
                new JobAnalysisResult("백엔드", "이유3", "스킬3")
        );
    }

    @Test
    void crawlsAndSavesWhenBothStagesSucceedOnFirstAttempt() {
        ArticleContentFetcher articleContentFetcher = mock(ArticleContentFetcher.class);
        OpenAiClient openAiClient = mock(OpenAiClient.class);
        NewsAnalysisSaver saver = mock(NewsAnalysisSaver.class);
        NewsAnalysisMapper mapper = mock(NewsAnalysisMapper.class);
        News news = newsWithId(1L);
        GeneralAnalysisResult general = sampleGeneral();
        List<JobAnalysisResult> jobs = sampleJobs();

        when(mapper.selectUnanalyzedNews(anyInt())).thenReturn(List.of(news));
        when(articleContentFetcher.fetch(news.getUrl(), news.getSource())).thenReturn("충분히 긴 크롤링 원문".repeat(20));
        when(openAiClient.analyzeGeneral(eq(news.getTitle()), any())).thenReturn(general);
        when(openAiClient.analyzeJobs(general.getSummary())).thenReturn(jobs);

        NewsStructuringService service = new NewsStructuringService(
                articleContentFetcher, openAiClient, saver, mapper, propertiesWith(10), permissiveFilter());

        service.structureAll();

        verify(articleContentFetcher, times(1)).fetch(news.getUrl(), news.getSource());
        verify(openAiClient, times(1)).analyzeGeneral(eq(news.getTitle()), any());
        verify(openAiClient, times(1)).analyzeJobs(general.getSummary());
        verify(saver, times(1)).save(1L, general, jobs);
    }

    @Test
    void skipsCrawlingWhenTitleIsExcluded() {
        ArticleContentFetcher articleContentFetcher = mock(ArticleContentFetcher.class);
        OpenAiClient openAiClient = mock(OpenAiClient.class);
        NewsAnalysisSaver saver = mock(NewsAnalysisSaver.class);
        NewsAnalysisMapper mapper = mock(NewsAnalysisMapper.class);
        News news = newsWithId(2L);
        news.setTitle("[속보] 코스피 급락");

        NewsFilterProperties filterProps = new NewsFilterProperties();
        filterProps.setExcludeTitleKeywords(List.of("코스피"));
        filterProps.setMinContentLength(0);

        when(mapper.selectUnanalyzedNews(anyInt())).thenReturn(List.of(news));

        NewsStructuringService service = new NewsStructuringService(
                articleContentFetcher, openAiClient, saver, mapper, propertiesWith(10),
                new NewsRelevanceFilter(filterProps));

        service.structureAll();

        verifyNoInteractions(articleContentFetcher);
        verifyNoInteractions(openAiClient);
        verify(mapper, times(1)).insertFilteredOut(2L);
        verifyNoInteractions(saver);
    }

    @Test
    void marksFilteredOutWhenCrawlFails() {
        ArticleContentFetcher articleContentFetcher = mock(ArticleContentFetcher.class);
        OpenAiClient openAiClient = mock(OpenAiClient.class);
        NewsAnalysisSaver saver = mock(NewsAnalysisSaver.class);
        NewsAnalysisMapper mapper = mock(NewsAnalysisMapper.class);
        News news = newsWithId(3L);

        when(mapper.selectUnanalyzedNews(anyInt())).thenReturn(List.of(news));
        when(articleContentFetcher.fetch(news.getUrl(), news.getSource()))
                .thenThrow(new ArticleFetchException("selector not found"));

        NewsStructuringService service = new NewsStructuringService(
                articleContentFetcher, openAiClient, saver, mapper, propertiesWith(10), permissiveFilter());

        service.structureAll();

        verifyNoInteractions(openAiClient);
        verify(mapper, times(1)).insertFilteredOut(3L);
        verifyNoInteractions(saver);
    }

    @Test
    void discardsGeneralResultWhenJobStageFailsAfterAllRetries() {
        ArticleContentFetcher articleContentFetcher = mock(ArticleContentFetcher.class);
        OpenAiClient openAiClient = mock(OpenAiClient.class);
        NewsAnalysisSaver saver = mock(NewsAnalysisSaver.class);
        NewsAnalysisMapper mapper = mock(NewsAnalysisMapper.class);
        News news = newsWithId(4L);
        GeneralAnalysisResult general = sampleGeneral();

        when(mapper.selectUnanalyzedNews(anyInt())).thenReturn(List.of(news));
        when(articleContentFetcher.fetch(news.getUrl(), news.getSource())).thenReturn("충분히 긴 크롤링 원문".repeat(20));
        when(openAiClient.analyzeGeneral(eq(news.getTitle()), any())).thenReturn(general);
        when(openAiClient.analyzeJobs(general.getSummary()))
                .thenThrow(new AiStructureException("boom"));

        NewsStructuringService service = new NewsStructuringService(
                articleContentFetcher, openAiClient, saver, mapper, propertiesWith(10), permissiveFilter());

        service.structureAll();

        // 1단계(analyzeGeneral)는 성공했지만, 2단계(analyzeJobs)가 재시도까지 전부
        // 실패했으므로 1단계 결과도 저장하지 않아야 한다(all-or-nothing).
        verify(openAiClient, times(1)).analyzeGeneral(eq(news.getTitle()), any());
        // 최초 시도 1회 + 재시도 3회 = 총 4회
        verify(openAiClient, times(4)).analyzeJobs(general.getSummary());
        verifyNoInteractions(saver);
    }
}
