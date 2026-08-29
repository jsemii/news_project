package com.jobnews.scrap;

import java.time.LocalDateTime;

/**
 * [전체 흐름에서의 위치] 스크랩 API(ScrapController)가 클라이언트에게 실제로 돌려주는
 * JSON 응답 항목입니다. 누구의 스크랩인지(userId)는 "내 스크랩"이라는 맥락상 굳이
 * 내려줄 필요가 없어서 뺐습니다. 불변 객체(생성자로만 값을 채움, setter 없음)라
 * MyBatis <constructor> 매핑을 쓰며, 숫자 필드는 primitive가 아니라 박싱 타입(Long)을
 * 씁니다 — <constructor> 매핑에서 primitive 매개변수를 가진 생성자를 못 찾는 문제를
 * 이미 겪었기 때문입니다(docs/troubleshooting.md 26번 항목).
 */
public class ScrapItem {

    private final Long id;
    private final Long newsId;
    private final LocalDateTime createdAt;

    public ScrapItem(Long id, Long newsId, LocalDateTime createdAt) {
        this.id = id;
        this.newsId = newsId;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getNewsId() {
        return newsId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
