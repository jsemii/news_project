package com.jobnews.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * [전체 흐름에서의 위치] "수집" 단계를 위한 기반 설비를 준비하는 설정 클래스입니다.
 * 이 클래스 자체는 뉴스를 수집하지 않고, 뉴스를 수집할 때 쓸 HTTP 클라이언트(=인터넷에서
 * 데이터를 가져오는 도구)인 WebClient를 미리 하나 만들어서 스프링 컨테이너에 등록해둡니다.
 * 실제로 이 WebClient를 사용해 RSS를 가져오는 곳은 collector 패키지의 RssFetcher입니다.
 */
// @Configuration: "이 클래스 안에 @Bean이 붙은 객체 생성 메서드가 들어있다"고 스프링에게 알려주는
// 어노테이션입니다. 안 쓰면 아래 webClient() 메서드가 스프링에 등록되지 않아서, RssFetcher가
// WebClient를 주입받지 못하고 앱이 시작할 때 에러가 납니다.
@Configuration
public class WebClientConfig {

    // [무엇을 받아서] 입력값이 없습니다.
    // [무엇을 하고] WebClient(HTTP 요청을 보내는 도구)를 기본 설정으로 하나 만듭니다.
    // [무엇을 돌려주는지] 만들어진 WebClient 객체를 돌려줍니다.
    // [왜 필요한지] WebClient는 new로 직접 만들 수도 있지만, 스프링 빈(컨테이너가 관리하는 객체)으로
    //              한 번만 만들어서 여러 곳(RssFetcher 등)에서 재사용하는 것이 더 효율적이기 때문입니다.
    // @Bean: "이 메서드가 반환하는 객체를 스프링 컨테이너에 등록해라"는 뜻입니다.
    // 안 쓰면 다른 클래스의 생성자에서 WebClient를 자동으로 주입받을 수 없습니다.
    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }
}
