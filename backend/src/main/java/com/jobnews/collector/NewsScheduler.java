package com.jobnews.collector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NewsScheduler {

    private static final Logger log = LoggerFactory.getLogger(NewsScheduler.class);

    private final NewsCollectorService newsCollectorService;

    public NewsScheduler(NewsCollectorService newsCollectorService) {
        this.newsCollectorService = newsCollectorService;
    }

    @Scheduled(fixedDelayString = "${collector.schedule.fixed-delay-ms}")
    public void collectNews() {
        log.info("Scheduled news collection started");
        newsCollectorService.collect();
        log.info("Scheduled news collection finished");
    }
}
