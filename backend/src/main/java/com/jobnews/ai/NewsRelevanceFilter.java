package com.jobnews.ai;

import com.jobnews.news.News;
import org.springframework.stereotype.Component;

/**
 * [전체 흐름에서의 위치] "AI 뉴스 구조화" 직전에 실행되는 "규칙 기반 1차 필터"입니다.
 * LLM을 전혀 호출하지 않고, 제목/본문 길이만 보고 "이 뉴스는 애초에 분석할 가치가
 * 있는가"를 판단합니다. NewsStructuringService가 미분석 뉴스를 하나씩 처리하기
 * 전에 이 클래스에 먼저 물어보고, "가치 없음"이면 OpenAI를 아예 호출하지 않습니다
 * (LLM 호출 비용 절감 + 취준생에게 의미 없는 시황 스팟 뉴스가 브리핑에 안 섞이게 함).
 */
@Component
public class NewsRelevanceFilter {

    private final NewsFilterProperties filterProperties;

    public NewsRelevanceFilter(NewsFilterProperties filterProperties) {
        this.filterProperties = filterProperties;
    }

    /**
     * [무엇을 받아서] 판단할 뉴스 하나를 받습니다.
     * [무엇을 하고] 1) 제목에 제외 키워드가 하나라도 포함되는지, 2) 본문이 너무 짧은지
     *              두 가지를 확인합니다. 둘 중 하나라도 해당하면 "가치가 낮다"고 판단합니다.
     * [무엇을 돌려주는지] AI로 분석할 가치가 있으면 true, 없으면 false.
     */
    public boolean isWorthAnalyzing(News news) {
        return !matchesExcludedKeyword(news) && !isTooShort(news);
    }

    private boolean matchesExcludedKeyword(News news) {
        String title = news.getTitle();
        if (title == null) {
            return false;
        }
        // if: 제외 키워드 목록(application.yml의 ai.filter.exclude-title-keywords)을
        // 하나씩 확인해서, 제목에 그 키워드가 부분 문자열로라도 포함되면 즉시 제외 판정합니다.
        for (String keyword : filterProperties.getExcludeTitleKeywords()) {
            if (title.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean isTooShort(News news) {
        String description = news.getDescription();
        if (description == null) {
            return true; // 크롤링에 실패해서 본문이 아예 없는 경우도 "너무 짧음"으로 취급합니다.
        }
        return description.length() < filterProperties.getMinDescriptionLength();
    }
}
