package com.jobnews.news;

import java.time.LocalDateTime;

/**
 * [전체 흐름에서의 위치] "저장" 단계에서 사용하는 뉴스 기사 하나를 표현하는 모델(도메인) 클래스입니다.
 * RSS에서 가져온 기사 한 건의 정보(제목, 링크, 어느 언론사인지, 언제 쓰였는지)를
 * 자바 객체 하나에 담아서, collector 패키지(수집)와 news 패키지(저장) 사이에서
 * 데이터를 주고받는 DTO(계층 간 데이터를 주고받기 위한 전달용 객체) 역할도 합니다.
 * ⚠️ 기사 본문(원문 전체)은 이 클래스도, DB의 news 테이블도 갖고 있지 않습니다 —
 * 저작권 문제 때문에, AI 구조화 단계(ai 패키지)가 필요할 때마다 원문 URL을 직접
 * 크롤링해서 메모리에서만 잠깐 쓰고 버리는 방식으로 바뀌었습니다
 * (docs/troubleshooting.md 참고).
 */
public class News {

    // DB의 news 테이블 id 컬럼과 짝을 이루는 값입니다. 새로 만든 객체는 아직 DB에 저장되지
    // 않았으므로 id가 없고(null), INSERT 이후에 DB가 자동으로 채번한 값이 여기에 채워집니다.
    private Long id;
    // 기사 원문 링크. "URL 중복 제거" 로직(NewsService.saveIfNew, DB의 UNIQUE 제약)이
    // 바로 이 값을 기준으로 동작합니다 — 같은 url을 가진 기사는 두 번 저장되지 않습니다.
    // AI 구조화 단계에서 원문을 크롤링할 때도 바로 이 url을 사용합니다.
    private String url;
    private String title;
    // 어느 RSS 소스(예: "전자신문", "연합뉴스")에서 가져온 기사인지 표시합니다.
    private String source;
    // 기사가 실제로 발행된 시각(RSS의 pubDate). RSS에 이 정보가 없으면 null일 수 있습니다.
    private LocalDateTime publishedAt;
    // 우리 시스템이 이 기사를 수집해서 저장한 시각. DB에서 기본값(now())으로 자동 채워집니다.
    private LocalDateTime collectedAt;

    // [무엇을 받아서] 아무것도 받지 않는 기본 생성자입니다.
    // [왜 필요한지] MyBatis가 DB 조회 결과를 News 객체로 변환할 때, 먼저 빈 객체를 만든 뒤
    //              setter(예: setTitle)로 값을 하나씩 채워 넣기 때문에 이 생성자가 필요합니다.
    public News() {
    }

    // [무엇을 받아서] 새로 수집한 기사의 url/title/source/publishedAt을 받습니다.
    // [무엇을 하고] 각 필드에 값을 채워 넣습니다. (id와 collectedAt은 아직 없으므로 비워둠 —
    //              id는 DB 저장 시, collectedAt은 DB의 기본값으로 채워짐)
    // [왜 필요한지] RssFetcher가 RSS 기사 하나를 News 객체로 변환할 때 이 생성자를 사용합니다.
    public News(String url, String title, String source, LocalDateTime publishedAt) {
        this.url = url;
        this.title = title;
        this.source = source;
        this.publishedAt = publishedAt;
    }

    // 아래부터는 각 필드를 읽고(getXxx) 쓰는(setXxx) 메서드들입니다.
    // MyBatis나 스프링 같은 라이브러리들이 객체의 값을 안전하게 읽고 쓰기 위해
    // 이런 형태(getter/setter)를 표준 규칙처럼 사용합니다.

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
