package com.jobnews.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * [전체 흐름에서의 위치] "AI 뉴스 구조화" 단계에서 OpenAI API를 어떻게 호출할지를 정의하는
 * 설정 클래스입니다. API 키, 모델 이름, 재시도 정책, 실행 주기 같은 값들을 코드에 직접
 * 적지 않고 application.yml의 openai.* 항목에서 읽어옵니다(RssSourceProperties,
 * CollectorRetryProperties와 같은 패턴).
 */
@Component
@ConfigurationProperties(prefix = "openai")
public class OpenAiProperties {

    // OpenAI API 키. application.yml에는 ${OPENAI_API_KEY}로 적혀 있어서, 실제 값은
    // 환경변수에서 읽어옵니다(비밀값을 코드/설정 파일에 직접 적지 않기 위함, AGENTS.md 규칙 7).
    private String apiKey;
    // OpenAI API 서버 주소. 보통 바꿀 일이 없지만, 다른 호환 서버(프록시 등)를 쓸 경우를
    // 대비해 하드코딩하지 않고 설정값으로 뺐습니다.
    private String baseUrl;
    // 사용할 모델 이름(예: gpt-4o-mini). 나중에 더 저렴하거나 더 성능 좋은 모델로 바꿀 때
    // 코드를 고칠 필요 없이 이 값만 바꾸면 됩니다.
    private String model;
    // 뉴스 원문(description)을 OpenAI에 보낼 때 앞에서부터 몇 글자까지만 자를지. 원문이
    // 길어질수록 API 호출 비용이 커지기 때문에 상한선을 둡니다.
    private int maxInputChars;
    // 구조화(스케줄 실행이든 수동 트리거든)를 한 번 호출할 때 최대 몇 건까지 처리할지.
    // 이 값이 없으면 미분석 뉴스가 아무리 많아도(예: 167건) 한 번의 실행이 전부 처리할
    // 때까지 끝나지 않아서, 수동 트리거 API의 응답이 지나치게 오래 걸리거나 OpenAI 비용이
    // 한 번에 몰릴 수 있습니다. 이 값을 넘는 나머지는 다음 실행(스케줄 또는 재호출) 때 처리됩니다.
    private int batchSize;
    private Retry retry = new Retry();
    private Schedule schedule = new Schedule();

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getMaxInputChars() {
        return maxInputChars;
    }

    public void setMaxInputChars(int maxInputChars) {
        this.maxInputChars = maxInputChars;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public Retry getRetry() {
        return retry;
    }

    public void setRetry(Retry retry) {
        this.retry = retry;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
    }

    /**
     * OpenAI 호출이 실패했을 때의 재시도 정책. RSS 수집(CollectorRetryProperties)과
     * 똑같은 모양이지만, 설정 경로("openai.retry")가 다르기 때문에 별도 클래스로 뒀습니다.
     */
    public static class Retry {
        private int maxAttempts;
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

    /** AI 구조화를 몇 밀리초마다 반복 실행할지(NewsScheduler의 collector.schedule과 같은 개념). */
    public static class Schedule {
        private long fixedDelayMs;

        public long getFixedDelayMs() {
            return fixedDelayMs;
        }

        public void setFixedDelayMs(long fixedDelayMs) {
            this.fixedDelayMs = fixedDelayMs;
        }
    }
}
