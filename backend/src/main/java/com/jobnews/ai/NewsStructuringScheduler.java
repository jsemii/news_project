package com.jobnews.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * [전체 흐름에서의 위치] "AI 뉴스 구조화" 단계의 시작 버튼을 정해진 시각에 자동으로
 * 눌러주는 클래스입니다. collector 패키지의 NewsScheduler와 같은 역할을, "수집"이
 * 아니라 "AI 분석"에 대해 담당합니다.
 */
@Component
public class NewsStructuringScheduler {

    private static final Logger log = LoggerFactory.getLogger(NewsStructuringScheduler.class);

    private final NewsStructuringService newsStructuringService;

    public NewsStructuringScheduler(NewsStructuringService newsStructuringService) {
        this.newsStructuringService = newsStructuringService;
    }

    // @Scheduled(cron = "..."): 정해진 "시각"에 실행하고 싶을 때 쓰는 방식입니다
    // (NewsScheduler의 fixedDelay는 "이전 실행이 끝난 뒤 몇 밀리초 후"라는 상대적인
    // 간격이라 하루 중 정확히 몇 시에 실행될지 예측할 수 없는데, cron은 "매일 08/13/18시"처럼
    // 절대 시각을 지정할 수 있습니다). 값은 application.yml의 openai.schedule.cron에서
    // 읽어옵니다(기본값: "0 0 8,13,18 * * *" = 매일 08:00/13:00/18:00, 초 분 시 일 월 요일 순서).
    @Scheduled(cron = "${openai.schedule.cron}")
    public void structureNews() {
        log.info("Scheduled AI structuring started");
        StructuringSummary summary = newsStructuringService.structureAll();
        log.info("Scheduled AI structuring finished: found={} filtered={} succeeded={} failed={} remainingBacklog={}",
                summary.getTotalFound(), summary.getFilteredOut(), summary.getSucceeded(), summary.getFailed(),
                summary.getRemainingBacklog());
    }
}
