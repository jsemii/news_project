package com.jobnews.ai;

import com.jobnews.news.News;
import org.springframework.stereotype.Component;

/**
 * [전체 흐름에서의 위치] "AI 뉴스 구조화" 직전에 실행되는 "규칙 기반 1차 필터"입니다.
 * LLM을 전혀 호출하지 않고 값싼 문자열 비교만으로 "이 뉴스는 애초에 분석할 가치가
 * 있는가"를 판단합니다. 두 가지 체크를 분리해서 제공하는 이유는 판단 시점이 다르기
 * 때문입니다 — 제목 키워드는 크롤링도 하기 전에(공짜로) 알 수 있지만, 본문 길이는
 * 크롤링을 해봐야만 알 수 있습니다. NewsStructuringService가 각 체크를 알맞은
 * 시점에 호출합니다(제목 체크 → 통과하면 크롤링 → 본문 길이 체크).
 */
@Component
public class NewsRelevanceFilter {

    private final NewsFilterProperties filterProperties;

    public NewsRelevanceFilter(NewsFilterProperties filterProperties) {
        this.filterProperties = filterProperties;
    }

    /**
     * [무엇을 받아서] 판단할 뉴스 하나를 받습니다.
     * [무엇을 하고] 제목에 제외 키워드(application.yml의 ai.filter.exclude-title-keywords)가
     *              하나라도 포함되는지 확인합니다. 크롤링 요청을 보내기 전에 부르는 게
     *              핵심입니다 — 여기서 걸러지면 그 언론사 페이지에 아예 요청을 보내지 않습니다.
     * [무엇을 돌려주는지] 제목만으로 이미 "가치 없음"이 확실하면 true, 아니면 false.
     */
    public boolean isTitleExcluded(News news) {
        String title = news.getTitle();
        if (title == null) {
            return false;
        }
        // if: 제외 키워드 목록을 하나씩 확인해서, 제목에 그 키워드가 부분 문자열로라도
        // 포함되면 즉시 제외 판정합니다.
        for (String keyword : filterProperties.getExcludeTitleKeywords()) {
            if (title.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * [무엇을 받아서] 방금 크롤링해서 얻은 원문 텍스트(메모리 상의 값, DB에 저장된 값이
     *              아님)를 받습니다.
     * [무엇을 하고] 길이가 ai.filter.min-content-length보다 짧은지 확인합니다. 크롤링
     *              자체에 실패해 원문이 아예 없는 경우(null)도 "너무 짧음"으로 취급합니다 —
     *              둘 다 "AI에게 넘길 만한 내용이 없다"는 점에서 같은 결론이기 때문입니다.
     * [무엇을 돌려주는지] 내용이 부족해서 분석할 가치가 없으면 true, 충분하면 false.
     */
    public boolean isContentTooShort(String crawledContent) {
        if (crawledContent == null) {
            return true;
        }
        return crawledContent.length() < filterProperties.getMinContentLength();
    }
}
