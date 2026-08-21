package com.jobnews.ai;

import com.jobnews.news.News;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NewsRelevanceFilterTest {

    private NewsRelevanceFilter filterWith(List<String> excludeKeywords, int minLength) {
        NewsFilterProperties props = new NewsFilterProperties();
        props.setExcludeTitleKeywords(excludeKeywords);
        props.setMinContentLength(minLength);
        return new NewsRelevanceFilter(props);
    }

    private News newsWithTitle(String title) {
        News news = new News();
        news.setTitle(title);
        return news;
    }

    @Test
    void excludesTitleContainingExcludedKeyword() {
        NewsRelevanceFilter filter = filterWith(List.of("코스피", "환율"), 200);
        News news = newsWithTitle("[속보] 코스피 5%대 급락");

        assertThat(filter.isTitleExcluded(news)).isTrue();
    }

    @Test
    void keepsTitleWithoutExcludedKeyword() {
        NewsRelevanceFilter filter = filterWith(List.of("코스피", "환율"), 200);
        News news = newsWithTitle("삼성전자, 차세대 AI 반도체 양산 돌입");

        assertThat(filter.isTitleExcluded(news)).isFalse();
    }

    @Test
    void treatsTooShortCrawledContentAsNotWorthAnalyzing() {
        NewsRelevanceFilter filter = filterWith(List.of(), 200);

        assertThat(filter.isContentTooShort("짧은 본문")).isTrue();
    }

    @Test
    void treatsNullCrawledContentAsNotWorthAnalyzing() {
        NewsRelevanceFilter filter = filterWith(List.of(), 200);

        assertThat(filter.isContentTooShort(null)).isTrue();
    }

    @Test
    void keepsSufficientlyLongCrawledContent() {
        NewsRelevanceFilter filter = filterWith(List.of(), 200);

        assertThat(filter.isContentTooShort("충분히 긴 본문 내용입니다. ".repeat(20))).isFalse();
    }
}
