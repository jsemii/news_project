package com.jobnews.scrap;

import java.time.LocalDateTime;

/**
 * [전체 흐름에서의 위치] 로그인한 사용자가 뉴스를 스크랩(북마크)했다는 사실 1건을
 * 나타내는 모델입니다. MyBatis가 INSERT/SELECT 결과를 이 클래스에 채워 넣을 때
 * setter를 쓰므로(User.java와 같은 패턴), 불변 DTO(ScrapItem)와 달리 기본 생성자와
 * setter를 갖춘 평범한 모델 클래스로 만듭니다.
 */
public class Scrap {

    private Long id;
    private Long userId;
    private Long newsId;
    private LocalDateTime createdAt;

    // 기본 생성자: MyBatis가 SELECT 결과를 setter로 채워 넣을 때 필요합니다.
    public Scrap() {
    }

    // [무엇을 받아서] 스크랩을 남길 사용자 id와 뉴스 id.
    // [무엇을 하고] 새로 INSERT할 스크랩 행을 만듭니다(id/createdAt은 DB가 채움).
    public Scrap(Long userId, Long newsId) {
        this.userId = userId;
        this.newsId = newsId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getNewsId() {
        return newsId;
    }

    public void setNewsId(Long newsId) {
        this.newsId = newsId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
