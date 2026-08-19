package com.jobnews.collector;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * [전체 흐름에서의 위치] "수집 실패 시 재시도" 규칙(몇 번까지, 몇 초씩 기다리며)을
 * 정의하는 설정 클래스입니다. RssSourceProperties와 마찬가지로 실제 값(3회,
 * 2초/4초/8초)은 application.yml의 collector.retry 항목에 있고, 이 클래스는
 * 그 값을 자바 객체로 옮겨 담기만 합니다. 실제 재시도 동작은
 * NewsCollectorService가 이 값을 읽어서 수행합니다.
 */
@Component
// @ConfigurationProperties(prefix = "collector.retry"): yml의 "collector: retry: ..."
// 아래 항목들을 이 클래스의 필드로 자동 매핑합니다.
@ConfigurationProperties(prefix = "collector.retry")
public class CollectorRetryProperties {

    // 최초 시도가 실패했을 때, 추가로 몇 번까지 다시 시도할지(재시도 횟수). yml 기본값 3.
    private int maxAttempts;
    // 재시도마다 몇 초씩 기다릴지의 목록. yml 기본값 [2, 4, 8] → 1번째 재시도 전 2초,
    // 2번째 재시도 전 4초, 3번째 재시도 전 8초 대기(지수 백오프: 실패할수록 대기 시간을
    // 점점 늘려서, 일시적으로 바쁜 서버에 계속 몰아치듯 요청하지 않도록 하는 방식).
    private List<Integer> backoffSeconds = new ArrayList<>();

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public List<Integer> getBackoffSeconds() {
        return backoffSeconds;
    }

    public void setBackoffSeconds(List<Integer> backoffSeconds) {
        this.backoffSeconds = backoffSeconds;
    }
}
