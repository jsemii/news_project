package com.jobnews.config;

import com.jobnews.auth.CustomOAuth2UserService;
import com.jobnews.auth.CustomOidcUserService;
import com.jobnews.auth.JwtAuthenticationFilter;
import com.jobnews.auth.OAuth2LoginSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * [전체 흐름에서의 위치] 이 서비스 전체의 보안(인증/인가) 규칙을 정하는 설정입니다.
 * 기존에 완전히 공개였던 API는 이 설정을 추가한 뒤에도 여전히 인증 없이 그대로
 * 동작합니다(permitAll 유지) — 지금은 접근을 막는 게 목적이 아니라 로그인 인프라만
 * 까는 단계입니다. GitHub는 CustomOAuth2UserService(일반 OAuth2), Google은
 * CustomOidcUserService(OIDC)로 각각 연결합니다(두 서비스 모두 내부적으로
 * UserProvisioningService를 호출해 "처음 로그인이 곧 가입"을 처리). 로그인 성공 시
 * OAuth2LoginSuccessHandler가 JWT를 발급해 쿠키로 심고, 그 이후 모든 요청은
 * JwtAuthenticationFilter가 그 쿠키로 로그인 상태를 판단합니다(서버는 세션에 아무것도
 * 저장하지 않습니다 — 배포마다 컨테이너가 재생성돼도 로그인이 유지되는 이유).
 * CSRF를 끈 이유: 이 서비스는 브라우저 폼이 아니라 curl/Swagger로 호출하는 수동
 * 트리거 API(POST /api/structuring/run 등)가 이미 있고, 로그인 사용자가 상태를
 * 바꾸는 기능(스크랩 등)은 아직 없습니다. 그 기능을 만들 때 다시 검토합니다.
 */
@Configuration
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOidcUserService customOidcUserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService,
                           CustomOidcUserService customOidcUserService,
                           OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
                           JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.customOAuth2UserService = customOAuth2UserService;
        this.customOidcUserService = customOidcUserService;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    // [무엇을 받아서] Spring Security가 요청마다 거치는 필터 체인을 구성할
    //              HttpSecurity 빌더를 받습니다.
    // [무엇을 하고] 모든 요청을 인증 없이 허용(permitAll)하고 CSRF는 끈 채로, GitHub/
    //              Google OAuth2 로그인을 연결합니다. userInfoEndpoint에 두 개의
    //              userService를 등록하는 이유: GitHub(비-OIDC)는 .userService(),
    //              Google(OIDC)은 .oidcUserService()로 완전히 다른 훅을 쓰기
    //              때문입니다 — 하나만 등록하면 다른 쪽 provider의 사용자 정보 조회가
    //              동작하지 않습니다. jwtAuthenticationFilter를
    //              UsernamePasswordAuthenticationFilter보다 앞에 두는 이유는, 스프링의
    //              나머지 인증 처리보다 먼저 우리 쿠키 기반 로그인 상태를 SecurityContext에
    //              채워 넣어야 그 뒤 단계들이 "이미 로그인됨"을 인식하기 때문입니다.
    // [무엇을 돌려주는지] 구성된 SecurityFilterChain 빈. Spring이 이걸 실제 요청
    //              처리에 사용합니다.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                                .oidcUserService(customOidcUserService))
                        .successHandler(oAuth2LoginSuccessHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
