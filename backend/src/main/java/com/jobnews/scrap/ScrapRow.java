package com.jobnews.scrap;

import java.time.LocalDateTime;

/**
 * [전체 흐름에서의 위치] "내 스크랩 목록" 조회(ScrapMapper.selectByUser)가 news/
 * news_industry를 조인해서 가져온 가공 전 행입니다. BriefingRow와 같은 역할 —
 * industriesCsv(콤마로 이어붙인 산업명 문자열)를 List&lt;String&gt;으로 쪼개는 것은
 * ScrapController가 담당합니다(MyBatis가 setter로 채우므로 mutable 모델입니다).
 */
public class ScrapRow {

    private Long id;
    private Long newsId;
    private String title;
    private String url;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private String industriesCsv;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNewsId() {
        return newsId;
    }

    public void setNewsId(Long newsId) {
        this.newsId = newsId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getIndustriesCsv() {
        return industriesCsv;
    }

    public void setIndustriesCsv(String industriesCsv) {
        this.industriesCsv = industriesCsv;
    }
}
