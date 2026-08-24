package com.jobnews.ai;

import com.jobnews.news.News;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NewsRelevanceFilterTest {

    private NewsRelevanceFilter filterWith(List<String> excludeKeywords, int minLength) {
        return filterWith(excludeKeywords, minLength, 2);
    }

    private NewsRelevanceFilter filterWith(List<String> excludeKeywords, int minLength, int maxAgeDays) {
        NewsFilterProperties filterProps = new NewsFilterProperties();
        filterProps.setExcludeTitleKeywords(excludeKeywords);
        filterProps.setMinContentLength(minLength);
        AiStructuringProperties structuringProps = new AiStructuringProperties();
        structuringProps.setMaxAgeDays(maxAgeDays);
        return new NewsRelevanceFilter(filterProps, structuringProps);
    }

    private News newsWithTitle(String title) {
        News news = new News();
        news.setTitle(title);
        return news;
    }

    private News newsCollectedAt(LocalDateTime collectedAt) {
        News news = new News();
        news.setCollectedAt(collectedAt);
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

    @Test
    void treatsNewsOlderThanMaxAgeDaysAsTooOld() {
        NewsRelevanceFilter filter = filterWith(List.of(), 200, 2);
        // 기준(2일)보다 하루 더 오래 수집된 뉴스 — 경계값 바로 바깥쪽.
        News news = newsCollectedAt(LocalDateTime.now().minusDays(3));

        assertThat(filter.isTooOld(news)).isTrue();
    }

    @Test
    void keepsNewsWithinMaxAgeDays() {
        NewsRelevanceFilter filter = filterWith(List.of(), 200, 2);
        // 기준(2일) 이내에 수집된 뉴스 — 경계값 바로 안쪽.
        News news = newsCollectedAt(LocalDateTime.now().minusDays(1));

        assertThat(filter.isTooOld(news)).isFalse();
    }

    @Test
    void treatsMissingCollectedAtAsNotTooOld() {
        NewsRelevanceFilter filter = filterWith(List.of(), 200, 2);
        // collectedAt이 없는 비정상 상태는 안전하게 "오래되지 않음"으로 취급한다
        // (섣불리 걸러내서 데이터를 잃지 않기 위함).
        News news = newsCollectedAt(null);

        assertThat(filter.isTooOld(news)).isFalse();
    }
}
