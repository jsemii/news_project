package com.jobnews.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

/**
 * [전체 흐름에서의 위치] 로그인 성공 후 우리 서버가 자체 발급하는 JWT를 만들고
 * (issueToken), 이후 요청마다 쿠키에 담겨 오는 JWT가 유효한지 검증(parseToken)하는
 * 곳입니다. 서버 세션(HttpSession) 대신 이 방식을 쓰는 이유는 JwtProperties 주석과
 * application.yml의 jwt 섹션 주석을 참고하세요(요약: 이 프로젝트는 배포할 때마다
 * backend 컨테이너가 재생성되는데, 메모리 기반 서버 세션이었다면 배포할 때마다
 * 로그인한 사용자가 전부 강제 로그아웃됩니다).
 */
@Service
public class JwtService {

    // OAuth2LoginSuccessHandler(쿠키를 심는 쪽)와 JwtAuthenticationFilter/AuthController
    // (쿠키를 읽거나 지우는 쪽)가 전부 이 이름을 함께 써야 하므로, 어느 한쪽에만 두지 않고
    // 이 클래스에 상수로 둡니다("JWT를 다루는 곳"이라는 의미에서 자연스러운 위치).
    public static final String COOKIE_NAME = "jobnews_auth";

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_NAME = "name";

    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    // [무엇을 받아서] 로그인에 성공한 사용자(User, id/email/name/role이 채워진 상태)를 받습니다.
    // [무엇을 하고] 사용자 id를 subject로, email/name/role을 클레임으로 담아 서명된 JWT
    //              문자열을 만듭니다. 만료 기한은 jwt.expiration-days(기본 7일) 뒤입니다.
    // [무엇을 돌려주는지] 서명된 JWT 문자열(쿠키 값으로 그대로 씀).
    public String issueToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(jwtProperties.getExpirationDays(), ChronoUnit.DAYS);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_NAME, user.getName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey())
                .compact();
    }

    // [무엇을 받아서] 쿠키에서 꺼낸 JWT 문자열(원본 그대로, 아직 검증 전)을 받습니다.
    // [무엇을 하고] 서명이 우리 비밀키와 일치하는지, 만료되지 않았는지 검증합니다.
    // [무엇을 돌려주는지] 유효하면 클레임(Claims, subject/role/email/name 포함)을 담은
    //              Optional, 서명이 위조됐거나 만료됐거나 형식이 잘못됐으면 빈 Optional
    //              (JwtAuthenticationFilter가 이 경우 "로그인 안 된 상태"로 조용히 넘어감
    //              — 잘못된 쿠키 하나 때문에 요청 전체가 500 에러로 죽으면 안 되므로).
    public Optional<Claims> parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
