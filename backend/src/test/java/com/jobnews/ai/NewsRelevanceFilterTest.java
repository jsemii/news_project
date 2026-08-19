package com.jobnews.ai;

import com.jobnews.news.News;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NewsRelevanceFilterTest {

    private NewsRelevanceFilter filterWith(List<String> excludeKeywords, int minLength) {
        NewsFilterProperties props = new NewsFilterProperties();
        props.setExcludeTitleKeywords(excludeKeywords);
        props.setMinDescriptionLength(minLength);
        return new NewsRelevanceFilter(props);
    }

    private News newsOf(String title, String description) {
        News news = new News();
        news.setTitle(title);
        news.setDescription(description);
        return news;
    }

    @Test
    void excludesNewsWhoseTitleContainsExcludedKeyword() {
        NewsRelevanceFilter filter = filterWith(List.of("코스피", "환율"), 0);
        News news = newsOf("[속보] 코스피 5%대 급락", "충분히 긴 본문".repeat(50));

        assertThat(filter.isWorthAnalyzing(news)).isFalse();
    }

    @Test
    void excludesNewsWithTooShortDescription() {
        NewsRelevanceFilter filter = filterWith(List.of(), 200);
        News news = newsOf("삼성전자 반도체 투자 확대", "짧은 본문");

        assertThat(filter.isWorthAnalyzing(news)).isFalse();
    }

    @Test
    void excludesNewsWithNullDescription() {
        NewsRelevanceFilter filter = filterWith(List.of(), 200);
        News news = newsOf("크롤링 실패한 기사", null);

        assertThat(filter.isWorthAnalyzing(news)).isFalse();
    }

    @Test
    void keepsNewsThatPassesBothChecks() {
        NewsRelevanceFilter filter = filterWith(List.of("코스피", "환율"), 200);
        News news = newsOf("삼성전자, 차세대 AI 반도체 양산 돌입", "충분히 긴 본문 내용입니다. ".repeat(20));

        assertThat(filter.isWorthAnalyzing(news)).isTrue();
    }
}
