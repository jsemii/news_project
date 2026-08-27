package com.jobnews.auth;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * [전체 흐름에서의 위치] "로그인된 상태"의 실제 기준입니다. OAuth2 로그인 핸드셰이크
 * 자체(리다이렉트 왕복 몇 초)는 스프링이 내부적으로 세션을 잠깐 쓰지만, 그 이후
 * 모든 요청에서 "지금 로그인돼 있는가"는 이 필터가 매번 쿠키(JwtService.COOKIE_NAME)의
 * JWT를 검증해서 판단합니다. 서버가 재시작돼도(재배포) 이 판단 방식은 영향을
 * 안 받습니다 — 서버 메모리에 아무것도 저장하지 않기 때문입니다.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    // [무엇을 받아서] 들어오는 요청, 나갈 응답, 다음 필터로 넘길 체인을 받습니다
    //              (모든 서블릿 필터의 공통 형태).
    // [무엇을 하고] 쿠키에서 JWT를 꺼내 검증합니다. 유효하면 SecurityContext에
    //              Authentication을 채워 넣어서, 이후 컨트롤러(@AuthenticationPrincipal
    //              등)와 SecurityConfig의 인가 규칙이 "로그인된 사용자"로 인식하게
    //              합니다. 쿠키가 없거나 유효하지 않으면 아무것도 안 하고 그냥
    //              다음 필터로 넘깁니다(에러로 막지 않음 — 이 서비스는 로그인 없이도
    //              쓸 수 있는 공개 API가 대부분이라, 여기서 막으면 안 됩니다).
    // [무엇을 돌려주는지] 반환값 없음(void) — 항상 filterChain.doFilter로 다음 단계에 넘깁니다.
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        extractCookie(request).flatMap(jwtService::parseToken).ifPresent(this::authenticate);
        filterChain.doFilter(request, response);
    }

    private Optional<String> extractCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        for (Cookie cookie : request.getCookies()) {
            if (JwtService.COOKIE_NAME.equals(cookie.getName())) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    // [무엇을 받아서] JwtService가 검증까지 마친 클레임(Claims)을 받습니다.
    // [무엇을 하고] 클레임 안의 id/email/name/role로 User 객체를 다시 조립하고
    //              (DB를 다시 조회하지 않습니다 — JWT 자체에 필요한 정보가 이미 다
    //              있으므로), role 기반 권한(ROLE_USER/ROLE_ADMIN)과 함께 SecurityContext에
    //              채워 넣습니다.
    private void authenticate(Claims claims) {
        User user = new User();
        user.setId(Long.valueOf(claims.getSubject()));
        user.setEmail(claims.get("email", String.class));
        user.setName(claims.get("name", String.class));
        user.setRole(Role.valueOf(claims.get("role", String.class)));

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
