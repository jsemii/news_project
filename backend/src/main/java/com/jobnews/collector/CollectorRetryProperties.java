package com.jobnews.collector;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "collector.retry")
public class CollectorRetryProperties {

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
