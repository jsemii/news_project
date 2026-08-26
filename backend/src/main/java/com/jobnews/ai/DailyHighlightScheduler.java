package com.jobnews.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * [전체 흐름에서의 위치] "오늘 한 줄 요약" 재계산의 시작 버튼을 정해진 시각에 자동으로
 * 눌러주는 클래스입니다. NewsStructuringScheduler(뉴스 구조화, 매시간)와 같은 역할을
 * "오늘 한 줄 요약"에 대해 담당하되, 완전히 독립된 스케줄로 동작합니다 — 구조화는
 * 처리한 만큼만 과금돼 매시간 돌려도 비용이 예측 가능하지만, 이 기능을 구조화 배치에
 * 얹어 돌리면(예전 방식) 배치마다 딸려가서 최대 하루 24번까지 LLM을 호출할 수 있어
 * 비용이 예측 불가능해집니다. 하루 3번 고정 시각으로 분리해 비용을 예측 가능하게 합니다.
 */
@Component
public class DailyHighlightScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyHighlightScheduler.class);

    private final DailyHighlightService dailyHighlightService;

    public DailyHighlightScheduler(DailyHighlightService dailyHighlightService) {
        this.dailyHighlightService = dailyHighlightService;
    }

    // @Scheduled(cron = "..."): application.yml의 daily-highlight.schedule.cron
    // (기본값 "0 30 8,13,18 * * *" = 매일 08:30/13:30/18:30)에서 값을 읽어옵니다.
    // zone = "Asia/Seoul"이 필요한 이유는 NewsStructuringScheduler와 동일합니다 —
    // 이게 없으면 컨테이너 기본 시간대(보통 UTC) 기준으로 실행돼 실제 원하는 시각과
    // 어긋납니다(docs/troubleshooting.md 참고). 예외를 이 메서드 안에서 따로 감싸지
    // 않는 이유도 NewsStructuringScheduler.structureNews()와 동일합니다 — Spring이
    // @Scheduled 메서드의 예외를 로그로만 남기고 다음 실행에는 영향을 주지 않는 기존
    // 동작을 그대로 신뢰합니다.
    @Scheduled(cron = "${daily-highlight.schedule.cron}", zone = "Asia/Seoul")
    public void recomputeHighlight() {
        log.info("Scheduled daily highlight recompute started");
        boolean computed = dailyHighlightService.recomputeForToday();
        log.info("Scheduled daily highlight recompute finished: computed={}", computed);
    }
}
