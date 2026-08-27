package com.jobnews.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * [전체 흐름에서의 위치] 로그인 JWT의 설정값을 담는 클래스입니다. secret을 코드에
 * 하드코딩하지 않고 application.yml(→ 실제로는 JWT_SECRET 환경변수)로 뺀 이유는
 * 다른 모든 비밀값(DB_PASSWORD, OPENAI_API_KEY)과 같은 원칙입니다. expirationDays를
 * 설정으로 뺀 이유는 "만료 기한을 좀 늘리자/줄이자" 같은 조정을 코드 배포 없이
 * 할 수 있게 하기 위함입니다.
 */
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    // JWT 서명에 쓰는 비밀키. HS256 알고리즘은 최소 256비트(32바이트) 이상의 키를
    // 요구하므로, JWT_SECRET 값은 충분히 긴 무작위 문자열이어야 합니다(예: openssl
    // rand -base64 32로 생성).
    private String secret;
    // 로그인 유지 기간(일). application.yml 기본값 7.
    private int expirationDays;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public int getExpirationDays() {
        return expirationDays;
    }

    public void setExpirationDays(int expirationDays) {
        this.expirationDays = expirationDays;
    }
}
