package com.jobnews.config;

import com.jobnews.auth.CustomOAuth2UserService;
import com.jobnews.auth.CustomOidcUserService;
import com.jobnews.auth.JwtAuthenticationFilter;
import com.jobnews.auth.OAuth2LoginSuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * [전체 흐름에서의 위치] 이 서비스 전체의 보안(인증/인가) 규칙을 정하는 설정입니다.
 * 대부분의 API는 여전히 인증 없이 그대로 동작합니다(permitAll 유지) — "/api/stats/**"
 * (관리자 통계 대시보드)만 로그인한 ADMIN 회원에게만 열려 있고, 나머지는 로그인
 * 인프라를 처음 깔았을 때와 같습니다. GitHub는 CustomOAuth2UserService(일반 OAuth2),
 * Google은 CustomOidcUserService(OIDC)로 각각 연결합니다(두 서비스 모두 내부적으로
 * UserProvisioningService를 호출해 "처음 로그인이 곧 가입"을 처리). 로그인 성공 시
 * OAuth2LoginSuccessHandler가 JWT를 발급해 쿠키로 심고, 그 이후 모든 요청은
 * JwtAuthenticationFilter가 그 쿠키로 로그인 상태(및 role)를 판단합니다(서버는
 * 세션에 아무것도 저장하지 않습니다 — 배포마다 컨테이너가 재생성돼도 로그인이
 * 유지되는 이유). CSRF를 끈 이유: 이 서비스는 브라우저 폼이 아니라 curl/Swagger로
 * 호출하는 수동 트리거 API(POST /api/structuring/run 등)가 이미 있고, 로그인
 * 사용자가 상태를 바꾸는 기능(스크랩 등)은 아직 없습니다. 그 기능을 만들 때 다시
 * 검토합니다.
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
    // [무엇을 하고] "/api/stats/**"(관리자 통계 대시보드)는 ROLE_ADMIN만 허용하고,
    //              나머지 요청은 전부 인증 없이 허용(permitAll)한 채로 GitHub/
    //              Google OAuth2 로그인을 연결합니다. requestMatchers로 구체적인
    //              규칙을 anyRequest()보다 먼저 적어야 합니다 — Spring Security는
    //              위에서부터 순서대로 첫 번째로 매칭되는 규칙을 적용하므로, 순서가
    //              바뀌면 anyRequest().permitAll()이 먼저 걸려 "/api/stats/**"에도
    //              그대로 적용돼버립니다. userInfoEndpoint에 두 개의 userService를
    //              등록하는 이유: GitHub(비-OIDC)는 .userService(), Google(OIDC)은
    //              .oidcUserService()로 완전히 다른 훅을 쓰기 때문입니다 — 하나만
    //              등록하면 다른 쪽 provider의 사용자 정보 조회가 동작하지 않습니다.
    //              exceptionHandling의 defaultAuthenticationEntryPointFor는 "로그인
    //              안 하고 /api/** 를 호출했을 때 어떻게 응답할지"를 재정의합니다 —
    //              이게 없으면 .oauth2Login()이 등록한 기본 동작(GitHub 로그인
    //              화면으로 302 리다이렉트)이 그대로 적용되는데, fetch로 호출하는
    //              JSON API 입장에서는 302가 아니라 403이 훨씬 자연스러운 응답이라
    //              바꿔줍니다. "/oauth2/authorization/{provider}"(실제 로그인
    //              시작 경로)는 "/api/" 밖에 있어서 이 설정과 무관하게 그대로
    //              동작합니다. jwtAuthenticationFilter를
    //              UsernamePasswordAuthenticationFilter보다 앞에 두는 이유는, 스프링의
    //              나머지 인증 처리보다 먼저 우리 쿠키 기반 로그인 상태를 SecurityContext에
    //              채워 넣어야 그 뒤 단계들이 "이미 로그인됨"을 인식하기 때문입니다.
    // [무엇을 돌려주는지] 구성된 SecurityFilterChain 빈. Spring이 이걸 실제 요청
    //              처리에 사용합니다.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // DB에서 role을 USER→ADMIN으로 바꿔도, 이미 로그인해서 들고 있는
                        // JWT의 role 클레임은 발급 시점 값 그대로입니다(JwtAuthenticationFilter가
                        // 매 요청마다 DB를 다시 조회하지 않으므로) — 승격된 사용자는 재로그인해야
                        // 새 JWT로 ADMIN 권한을 실제로 받습니다(Role.java 주석 참고).
                        .requestMatchers("/api/stats/**").hasRole("ADMIN")
                        .anyRequest().permitAll())
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .defaultAuthenticationEntryPointFor(
                                (request, response, authException) -> response.sendError(HttpServletResponse.SC_FORBIDDEN),
                                PathPatternRequestMatcher.pathPattern("/api/**")))
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                                .oidcUserService(customOidcUserService))
                        .successHandler(oAuth2LoginSuccessHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
