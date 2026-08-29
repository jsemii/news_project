package com.jobnews.scrap;

import java.time.LocalDateTime;
import java.util.List;

/**
 * [전체 흐름에서의 위치] 스크랩 API(ScrapController)가 클라이언트에게 실제로 돌려주는
 * JSON 응답 항목입니다. 누구의 스크랩인지(userId)는 "내 스크랩"이라는 맥락상 굳이
 * 내려줄 필요가 없어서 뺐습니다. 불변 객체(생성자로만 값을 채움, setter 없음)입니다.
 * title/url/publishedAt/industries는 "내 리포트" 페이지의 최근 스크랩 목록에
 * 쓰려고 news/news_industry를 조인해서 채운 값입니다(스크랩 추가/취소 응답에서는
 * 조인을 안 해서 이 필드들이 null/빈 리스트일 수 있습니다 — ScrapController의
 * toItem(Scrap) 오버로드 참고).
 */
public class ScrapItem {

    private final Long id;
    private final Long newsId;
    private final String title;
    private final String url;
    private final LocalDateTime publishedAt;
    private final LocalDateTime createdAt;
    private final List<String> industries;

    public ScrapItem(Long id, Long newsId, String title, String url, LocalDateTime publishedAt,
                      LocalDateTime createdAt, List<String> industries) {
        this.id = id;
        this.newsId = newsId;
        this.title = title;
        this.url = url;
        this.publishedAt = publishedAt;
        this.createdAt = createdAt;
        this.industries = industries;
    }

    public Long getId() {
        return id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<String> getIndustries() {
        return industries;
    }
}
