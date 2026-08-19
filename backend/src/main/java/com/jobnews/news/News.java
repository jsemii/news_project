package com.jobnews.news;

import java.time.LocalDateTime;

public class News {

    private Long id;
    private String url;
    private String title;
    private String description;
    private String source;
    private LocalDateTime publishedAt;
    private LocalDateTime collectedAt;

    public News() {
    }

    public News(String url, String title, String description, String source, LocalDateTime publishedAt) {
        this.url = url;
        this.title = title;
        this.description = description;
        this.source = source;
        this.publishedAt = publishedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(LocalDateTime collectedAt) {
        this.collectedAt = collectedAt;
    }
}
