package com.jobnews.ai;

import com.jobnews.news.News;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class NewsStructuringServiceTest {

    private OpenAiProperties retryProperties() {
        OpenAiProperties props = new OpenAiProperties();
        props.setBatchSize(10); // 이 테스트들은 뉴스 1건짜리 시나리오만 다뤄서 값 자체는 중요하지 않음
        OpenAiProperties.Retry retry = new OpenAiProperties.Retry();
        retry.setMaxAttempts(3);
        retry.setBackoffSeconds(List.of(0, 0, 0)); // 테스트에서는 대기 없이 즉시 재시도
        props.setRetry(retry);
        return props;
    }

    // NewsRelevanceFilter가 이 테스트의 관심사(재시도 로직)를 방해하지 않도록, 아무것도
    // 걸러내지 않는 "통과 전용" 필터 설정을 만듭니다. 필터 자체의 동작은 NewsRelevanceFilterTest에서 검증합니다.
    private NewsRelevanceFilter permissiveFilter() {
        NewsFilterProperties props = new NewsFilterProperties();
        props.setExcludeTitleKeywords(List.of());
        props.setMinDescriptionLength(0);
        return new NewsRelevanceFilter(props);
    }

    // 반대로 뭐든 걸러내는 필터입니다. 필터링 경로(news_filtered_out에 표시가 남는지)를
    // 검증하는 데 씁니다.
    private NewsRelevanceFilter rejectAllFilter() {
        NewsFilterProperties props = new NewsFilterProperties();
        props.setExcludeTitleKeywords(List.of("테스트")); // newsWithId()가 만드는 제목("테스트 뉴스")에 항상 걸림
        props.setMinDescriptionLength(0);
        return new NewsRelevanceFilter(props);
    }

    private News newsWithId(long id) {
        News news = new News();
        news.setId(id);
        news.setTitle("테스트 뉴스");
        news.setDescription("테스트 본문");
        return news;
    }

    private AiAnalysisResult sampleResult() {
        return new AiAnalysisResult(
                "요약",
                List.of("금융"),
                List.of(
                        new AiAnalysisResult.JobAnalysis("IT전산", "이유1", "스킬1"),
                        new AiAnalysisResult.JobAnalysis("데이터분석", "이유2", "스킬2"),
                        new AiAnalysisResult.JobAnalysis("백엔드", "이유3", "스킬3")
                ),
                7,
                "테스트 이유"
        );
    }

    @Test
    void savesWhenFirstAttemptSucceeds() {
        OpenAiClient openAiClient = mock(OpenAiClient.class);
        NewsAnalysisSaver saver = mock(NewsAnalysisSaver.class);
        NewsAnalysisMapper mapper = mock(NewsAnalysisMapper.class);
        News news = newsWithId(1L);
        AiAnalysisResult result = sampleResult();

        when(mapper.selectUnanalyzedNews(anyInt())).thenReturn(List.of(news));
        when(openAiClient.analyze(news)).thenReturn(result);

        NewsStructuringService service = new NewsStructuringService(
                openAiClient, saver, mapper, retryProperties(), permissiveFilter());

        service.structureAll();

        verify(openAiClient, times(1)).analyze(news);
        verify(saver, times(1)).save(eq(1L), eq(result));
    }

    @Test
    void retriesOnFailureAndSavesBeforeExhaustingAttempts() {
        OpenAiClient openAiClient = mock(OpenAiClient.class);
        NewsAnalysisSaver saver = mock(NewsAnalysisSaver.class);
        NewsAnalysisMapper mapper = mock(NewsAnalysisMapper.class);
        News news = newsWithId(2L);
        AiAnalysisResult result = sampleResult();

        when(mapper.selectUnanalyzedNews(anyInt())).thenReturn(List.of(news));
        when(openAiClient.analyze(news))
                .thenThrow(new AiStructureException("boom"))
                .thenThrow(new AiStructureException("boom"))
                .thenReturn(result);

        NewsStructuringService service = new NewsStructuringService(
                openAiClient, saver, mapper, retryProperties(), permissiveFilter());

        service.structureAll();

        verify(openAiClient, times(3)).analyze(news);
        verify(saver, times(1)).save(eq(2L), eq(result));
    }

    @Test
    void givesUpAfterMaxAttemptsAndSkipsSaving() {
        OpenAiClient openAiClient = mock(OpenAiClient.class);
        NewsAnalysisSaver saver = mock(NewsAnalysisSaver.class);
        NewsAnalysisMapper mapper = mock(NewsAnalysisMapper.class);
        News news = newsWithId(3L);

        when(mapper.selectUnanalyzedNews(anyInt())).thenReturn(List.of(news));
        when(openAiClient.analyze(news)).thenThrow(new AiStructureException("boom"));

        NewsStructuringService service = new NewsStructuringService(
                openAiClient, saver, mapper, retryProperties(), permissiveFilter());

        service.structureAll();

        // 최초 시도 1회 + 재시도 3회 = 총 4회
        verify(openAiClient, times(4)).analyze(news);
        verifyNoInteractions(saver);
    }

    @Test
    void marksFilteredNewsAsFilteredOutSoItIsNotFetchedAgain() {
        OpenAiClient openAiClient = mock(OpenAiClient.class);
        NewsAnalysisSaver saver = mock(NewsAnalysisSaver.class);
        NewsAnalysisMapper mapper = mock(NewsAnalysisMapper.class);
        News news = newsWithId(4L);

        when(mapper.selectUnanalyzedNews(anyInt())).thenReturn(List.of(news));

        NewsStructuringService service = new NewsStructuringService(
                openAiClient, saver, mapper, retryProperties(), rejectAllFilter());

        service.structureAll();

        // 필터에 걸렸으니 OpenAI는 호출되지 않아야 하고, news_filtered_out에는 표시가
        // 남아야 합니다 — 이 표시가 없으면 다음 배치에서 같은 뉴스를 영원히 다시 가져오게
        // 됩니다(실제로 겪었던 무한 반복 버그).
        verifyNoInteractions(openAiClient);
        verify(mapper, times(1)).insertFilteredOut(4L);
        verifyNoInteractions(saver);
    }
}
