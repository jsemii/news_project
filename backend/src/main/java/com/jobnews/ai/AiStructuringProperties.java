package com.jobnews.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * [전체 흐름에서의 위치] "AI 뉴스 구조화" 대상을 고를 때 쓰는 설정값을 담는
 * 클래스입니다. 지금은 "수집된 지 며칠이 지나면 더 이상 분석하지 않을지" 하나뿐입니다.
 * 값을 코드에 하드코딩하지 않고 application.yml의 ai.structuring.max-age-days로
 * 뺀 이유는, 운영 중 "3일까지는 봐주자"처럼 기준을 조정할 때 코드 배포 없이 설정만
 * 바꾸면 되게 하기 위함입니다.
 */
@Component
@ConfigurationProperties(prefix = "ai.structuring")
public class AiStructuringProperties {

    // 뉴스가 수집(collected_at)된 지 이 값(일 단위)보다 오래되면, AI 구조화를 하지 않고
    // 건너뜁니다(news_filtered_out에 TOO_OLD로 표시). application.yml 기본값 2.
    private int maxAgeDays;

    public int getMaxAgeDays() {
        return maxAgeDays;
    }

    public void setMaxAgeDays(int maxAgeDays) {
        this.maxAgeDays = maxAgeDays;
    }
}
