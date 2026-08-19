package com.jobnews;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * [전체 흐름에서의 위치] 이 프로젝트 전체의 "시작점(진입점)" 클래스입니다.
 * 서비스 흐름(수집→저장→AI가공→브리핑조회) 중 특정 단계를 담당하지 않고,
 * 그 모든 단계를 담고 있는 Spring Boot 애플리케이션 자체를 켜고 끄는 역할을 합니다.
 * 이 클래스의 main() 메서드가 실행되면, collector/news/config 등 다른 패키지의
 * 클래스들이 전부 스프링 컨테이너(각 클래스의 객체를 만들고 관리해주는 상자)에 등록됩니다.
 */
// @SpringBootApplication: "이 클래스가 Spring Boot 앱의 시작점이다"라고 표시하는 어노테이션입니다.
// 이 한 줄 덕분에 컴포넌트 스캔(=com.jobnews 하위의 @Component, @Service 등을 자동으로 찾아서 등록),
// 자동 설정(=DB 연결, 웹 서버 등을 알아서 세팅)이 한 번에 켜집니다.
// 안 쓰면 각 기능을 전부 수동으로 하나하나 설정해줘야 합니다.
@SpringBootApplication
public class BackendApplication {

    // [무엇을 받아서] 프로그램 실행 시 커맨드라인 인자(args)를 받습니다.
    // [무엇을 하고] SpringApplication.run()을 호출해서 스프링 컨테이너를 띄우고,
    //              내장 톰캣(웹 서버)을 시작하고, @Scheduled 스케줄러도 함께 가동합니다.
    // [무엇을 돌려주는지] 반환값은 없습니다(void). 이 메서드가 끝나지 않고 계속 떠 있는 상태가
    //              바로 "서버가 실행 중"인 상태입니다.
    // [왜 필요한지] 자바 프로그램은 반드시 main() 메서드에서 시작해야 하기 때문에, 이 메서드가
    //              곧 "우리 서비스 전체의 전원 스위치"입니다.
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

}
