package com.jobnews.collector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * [전체 흐름에서의 위치] "수집" 단계의 시작 버튼을 자동으로 눌러주는 클래스입니다.
 * 사람이 매번 수동으로 실행하지 않아도, 정해진 주기(application.yml의
 * collector.schedule.fixed-delay-ms, 기본 30분)마다 NewsCollectorService.collect()를
 * 자동으로 호출해서 "수집→저장" 전체 파이프라인이 계속 돌아가게 합니다.
 */
// @Component: 이 클래스의 객체를 스프링이 만들어서 관리하게 합니다. @Scheduled가
// 동작하려면 이 객체가 스프링 컨테이너 안에 있어야 하므로 반드시 필요합니다.
@Component
public class NewsScheduler {

    private static final Logger log = LoggerFactory.getLogger(NewsScheduler.class);

    private final NewsCollectorService newsCollectorService;

    public NewsScheduler(NewsCollectorService newsCollectorService) {
        this.newsCollectorService = newsCollectorService;
    }

    // @Scheduled(fixedDelayString = "..."): 이 메서드를 "일정 주기마다 자동으로 반복 실행"
    // 하게 만드는 어노테이션입니다. fixedDelayString은 "이전 실행이 끝난 시점부터 몇 밀리초
    // 후에 다음 실행을 시작할지"를 의미하며, 값은 하드코딩하지 않고 application.yml의
    // collector.schedule.fixed-delay-ms를 읽어옵니다(현재 1800000ms = 30분). fixedDelay를
    // 쓴 이유는, 수집이 오래 걸려도 다음 수집과 겹치지 않고 항상 "이전 수집이 끝난 뒤"
    // 일정 시간을 기다리게 하기 위해서입니다. 이 어노테이션이 없으면 이 메서드는 그냥
    // 평범한 public 메서드일 뿐이라 아무도 자동으로 호출해주지 않습니다(수동 호출만 가능).
    // 참고: config 패키지의 SchedulerConfig에 있는 @EnableScheduling이 꺼져 있으면
    // 이 어노테이션이 있어도 무시됩니다.
    @Scheduled(fixedDelayString = "${collector.schedule.fixed-delay-ms}")
    public void collectNews() {
        // [무엇을 받아서] 입력값 없음(스케줄러가 시간이 되면 자동으로 호출).
        // [무엇을 하고] 실행 시작/종료를 로그로 남기고, 그 사이에 실제 수집·저장 로직
        //              전체(NewsCollectorService.collect())를 실행합니다.
        // [무엇을 돌려주는지] 반환값 없음(void) — 스케줄러가 호출하는 메서드는 결과를
        //              돌려받을 대상이 없으므로 void여야 합니다.
        log.info("Scheduled news collection started");
        newsCollectorService.collect();
        log.info("Scheduled news collection finished");
    }
}
