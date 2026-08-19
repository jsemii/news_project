package com.jobnews.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * [전체 흐름에서의 위치] "AI 뉴스 구조화" 전에 거치는 "규칙 기반 사전 필터"의 기준값을
 * 담는 설정 클래스입니다. "[속보] 코스피 급락" 같은 짧은 시황 스팟 뉴스는 취준생
 * 브리핑에 가치가 낮은데도 그대로 AI에게 보내면 LLM 호출 비용만 늘어나기 때문에,
 * OpenAI를 부르기 전에 값싼 문자열 비교로 먼저 걸러냅니다(NewsRelevanceFilter가 실제
 * 판단 로직을 담당). 제외 기준을 코드에 하드코딩하지 않고 여기(그리고 application.yml)로
 * 뺀 이유는, 나중에 "이 키워드도 걸러야겠다" 싶을 때 코드를 고치지 않고 설정만
 * 바꾸면 되게 하기 위함입니다.
 */
@Component
@ConfigurationProperties(prefix = "ai.filter")
public class NewsFilterProperties {

    // 제목에 이 목록 중 하나라도 포함되면 제외합니다(예: "코스피", "[인사]").
    private List<String> excludeTitleKeywords = new ArrayList<>();
    // 본문(description) 길이가 이 값보다 짧으면 제외합니다. 내용이 거의 없는 기사는
    // AI가 분석해도 근거가 부족한 결과만 나오기 때문입니다.
    private int minDescriptionLength;

    public List<String> getExcludeTitleKeywords() {
        return excludeTitleKeywords;
    }

    public void setExcludeTitleKeywords(List<String> excludeTitleKeywords) {
        this.excludeTitleKeywords = excludeTitleKeywords;
    }

    public int getMinDescriptionLength() {
        return minDescriptionLength;
    }

    public void setMinDescriptionLength(int minDescriptionLength) {
        this.minDescriptionLength = minDescriptionLength;
    }
}
