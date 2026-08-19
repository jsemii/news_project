package com.jobnews.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * [전체 흐름에서의 위치] "수집" 단계가 "주기적으로 자동 실행"되게 만드는 스위치 역할의
 * 설정 클래스입니다. 실제로 몇 분마다 무엇을 할지는 collector 패키지의 NewsScheduler에
 * 적혀 있고, 이 클래스는 그 예약 기능 자체를 켜는 일만 합니다.
 */
// @EnableScheduling: 스프링의 "예약 실행" 기능을 켜는 어노테이션입니다.
// 이걸 켜야만 다른 클래스에 있는 @Scheduled(예: NewsScheduler.collectNews())가 실제로
// 정해진 시간마다 자동 실행됩니다. 안 쓰면 @Scheduled를 아무리 붙여도 그냥 무시되고
// 아무 일도 일어나지 않습니다(수동으로 메서드를 호출해야만 실행됨).
@Configuration
@EnableScheduling
public class SchedulerConfig {
}
