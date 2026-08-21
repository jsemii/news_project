package com.jobnews.briefing;

import java.time.LocalDateTime;
import java.util.List;

/**
 * [전체 흐름에서의 위치] "직무별 맞춤 브리핑" 조회 API(BriefingController)가 실제로
 * 클라이언트(프론트엔드)에게 JSON으로 돌려주는 응답 항목입니다. 기사 원문은 애초에
 * 어떤 테이블에도 저장되지 않으므로 여기 포함될 수도 없고, AI가 사실관계를 재구성해
 * 만든 summary(짧은 재요약)만 내보냅니다(저작권 리스크 완화).
 */
public class BriefingItem {

    private final Long newsId;
    private final String title;
    private final String url;
    private final LocalDateTime publishedAt;
    private final String summary;
    private final int importanceScore;
    private final List<String> industries;

    public BriefingItem(Long newsId, String title, String url, LocalDateTime publishedAt,
                         String summary, int importanceScore, List<String> industries) {
        this.newsId = newsId;
        this.title = title;
        this.url = url;
        this.publishedAt = publishedAt;
        this.summary = summary;
        this.importanceScore = importanceScore;
        this.industries = industries;
    }

    public Long getNewsId() {
        return newsId;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public String getSummary() {
        return summary;
    }

    public int getImportanceScore() {
        return importanceScore;
    }

    public List<String> getIndustries() {
        return industries;
    }
}
