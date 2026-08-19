package com.jobnews.ai;

/**
 * [전체 흐름에서의 위치] "AI 뉴스 구조화" 단계에서 뉴스 하나에 태깅된 "관련 산업" 하나를
 * 표현하는 모델 클래스입니다. 뉴스 하나는 여러 산업과 관련될 수 있어서(예: "반도체 수출
 * 규제" 기사는 반도체 산업과 제조업 모두에 해당), news_industry 테이블에는 뉴스 1건당
 * 이 클래스의 객체가 여러 개(0개 이상) 행으로 저장됩니다. 산업을 콤마로 이어붙인 문자열
 * 하나로 저장하지 않고, "뉴스-산업" 쌍 하나하나를 별도 행으로 정규화해서, 나중에
 * "이 산업 관련 뉴스만 모아보기" 같은 조회를 SQL로 간단히 할 수 있게 했습니다.
 */
public class NewsIndustry {

    private Long id;
    // 이 산업 태그가 어느 뉴스에 붙은 것인지 가리키는 외래키 값입니다.
    private Long newsId;
    // 산업 이름. application.yml의 ai.industries에 정의된 8개 값(금융, 제조, 반도체 등) 중
    // 하나만 들어옵니다 — 임의의 값이 아니라 정해진 목록 안에서만 고르도록 프롬프트를 설계할
    // 예정입니다(Phase 4).
    private String industry;

    public NewsIndustry() {
    }

    // [무엇을 받아서] 이 산업 태그가 속한 뉴스 id와, 산업 이름을 받습니다.
    // [왜 필요한지] NewsStructuringService가 OpenAI 응답의 industries 목록을 순회하면서
    //              이 뉴스에 대한 NewsIndustry 객체를 하나씩 만들 때 사용합니다.
    public NewsIndustry(Long newsId, String industry) {
        this.newsId = newsId;
        this.industry = industry;
    }

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

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }
}
