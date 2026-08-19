package com.jobnews.collector;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * [전체 흐름에서의 위치] "수집" 단계 중 "기사 원문 크롤링"이 어느 CSS 셀렉터(HTML에서
 * 특정 태그를 짚어내는 규칙)로 본문을 찾을지를 정의하는 설정 클래스입니다.
 * 언론사(source)마다 홈페이지 구조가 다르기 때문에, "이 언론사는 이 셀렉터"라는
 * 매핑표를 application.yml의 crawler.selectors에 두고, 이 클래스가 그대로 옮겨 담습니다.
 * 실제로 이 값을 사용해 크롤링하는 곳은 ArticleContentFetcher입니다.
 */
@Component
// @ConfigurationProperties(prefix = "crawler"): yml의 "crawler: selectors: ..." 아래
// 목록을 이 클래스의 selectors 필드로 자동 매핑합니다.
@ConfigurationProperties(prefix = "crawler")
public class ArticleSelectorProperties {

    // yml의 crawler.selectors 리스트(언론사 이름 + 그 언론사의 본문 CSS 셀렉터 쌍)가 담깁니다.
    private List<Selector> selectors = new ArrayList<>();

    public List<Selector> getSelectors() {
        return selectors;
    }

    public void setSelectors(List<Selector> selectors) {
        this.selectors = selectors;
    }

    /**
     * [무엇을 받아서] 언론사 이름(source, 예: "전자신문")을 받습니다.
     * [무엇을 하고] 등록된 selectors 목록 중 이름이 일치하는 항목을 찾습니다.
     * [무엇을 돌려주는지] 찾은 CSS 셀렉터 문자열. 등록되지 않은 언론사면 null을 돌려줍니다
     *              (ArticleContentFetcher가 이 경우를 "크롤링 실패"로 처리합니다).
     * [왜 필요한지] "언론사 이름으로 셀렉터를 찾는다"는 조회 로직이 여러 곳에서 반복되지
     *              않도록, 이 설정 클래스 스스로 조회 기능을 제공합니다.
     */
    public String selectorFor(String source) {
        for (Selector selector : selectors) {
            if (selector.getName().equals(source)) {
                return selector.getSelector();
            }
        }
        return null;
    }

    /**
     * 언론사 하나(예: "전자신문")와, 그 언론사 기사 페이지에서 본문을 찾을 때 쓸
     * CSS 셀렉터(예: "#articleBody") 쌍을 표현하는 작은 데이터 묶음입니다.
     */
    public static class Selector {
        private String name;
        private String selector;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSelector() {
            return selector;
        }

        public void setSelector(String selector) {
            this.selector = selector;
        }
    }
}
