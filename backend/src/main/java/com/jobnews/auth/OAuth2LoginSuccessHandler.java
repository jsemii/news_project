package com.jobnews.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

/**
 * [전체 흐름에서의 위치] GitHub/Google 로그인이 성공한 바로 그 순간(단 한 번) 실행됩니다.
 * SecurityConfig.oauth2Login()의 successHandler로 등록되어, CustomOAuth2UserService/
 * CustomOidcUserService가 만든 AppOAuth2User/AppOidcUser(AppPrincipal 공통 인터페이스로
 * 꺼냄)에서 우리 User를 꺼내 JWT를 발급하고 쿠키로 심은 뒤, 프론트("/")로 돌려보냅니다.
 * 이 시점 이후로는 이 핸들러가 아니라 JwtAuthenticationFilter가 "로그인 상태"를 판단합니다.
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public OAuth2LoginSuccessHandler(JwtService jwtService, JwtProperties jwtProperties) {
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    // [무엇을 받아서] 방금 로그인에 성공한 요청/응답과, 그 로그인 결과(Authentication —
    //              principal이 AppOAuth2User 또는 AppOidcUser)를 받습니다.
    // [무엇을 하고] principal에서 우리 User를 꺼내 JWT를 발급하고, HttpOnly+Secure+
    //              SameSite=Lax 쿠키로 응답에 심습니다. secure 플래그는
    //              request.isSecure()를 그대로 따라갑니다 — 운영(HTTPS, nginx가
    //              X-Forwarded-Proto를 보내고 server.forward-headers-strategy가
    //              이를 신뢰)에서는 true, 로컬 개발(평문 HTTP)에서는 false가 되어
    //              로컬 브라우저에서도 쿠키가 정상적으로 저장됩니다. SameSite=Lax인
    //              이유: OAuth 로그인은 GitHub/Google에서 우리 사이트로 돌아오는
    //              "다른 사이트發 최상위 이동(top-level navigation)"인데, SameSite=Strict면
    //              이때 쿠키가 아예 전송되지 않아 로그인 직후 첫 요청에서 로그인이
    //              끊긴 것처럼 보일 수 있습니다. Lax는 이런 일반 이동은 허용하면서도
    //              CSRF 위험이 큰 교차 사이트 POST 등은 계속 막아줍니다.
    // [무엇을 돌려주는지] 반환값 없음 — 응답으로 "/"(프론트 SPA)로 리다이렉트합니다.
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException {
        User user = extractAppUser(authentication);
        if (user != null) {
            String token = jwtService.issueToken(user);
            ResponseCookie cookie = ResponseCookie.from(JwtService.COOKIE_NAME, token)
                    .httpOnly(true)
                    .secure(request.isSecure())
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(Duration.ofDays(jwtProperties.getExpirationDays()))
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        response.sendRedirect("/");
    }

    private User extractAppUser(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof AppPrincipal appPrincipal) {
            return appPrincipal.getAppUser();
        }
        return null;
    }
}
