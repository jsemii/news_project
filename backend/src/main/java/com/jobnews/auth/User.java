package com.jobnews.auth;

import java.time.LocalDateTime;

/**
 * [전체 흐름에서의 위치] GitHub/Google OAuth2 로그인으로 생긴 사용자 계정 1건을
 * 표현하는 모델 클래스입니다. users 테이블 1행과 대응합니다. 별도 회원가입 절차 없이
 * UserProvisioningService가 첫 로그인 시 자동으로 만들거나(없으면 insert), 기존 계정을
 * 그대로 재사용합니다(있으면 select).
 */
public class User {

    private Long id;
    // "github" 또는 "google". AiTaxonomyProperties의 산업/직무처럼 정해진 목록이지만,
    // OAuth2 registrationId를 그대로 쓰는 값이라 별도 enum 없이 문자열로 둡니다.
    private String provider;
    // 그 provider가 발급한 고유 ID(GitHub의 사용자 id, Google의 sub 클레임).
    private String providerId;
    private String email;
    private String name;
    private Role role;
    private LocalDateTime createdAt;

    public User() {
    }

    // [무엇을 받아서] 새로 가입하는 사용자에게 필요한 정보(provider, providerId, email, name)를
    //              받습니다. role/id/createdAt은 DB 기본값(USER/자동증가/now())으로 채워지므로
    //              여기서 받지 않습니다.
    // [왜 필요한지] UserProvisioningService가 처음 보는 사용자를 insert할 때 사용합니다.
    public User(String provider, String providerId, String email, String name) {
        this.provider = provider;
        this.providerId = providerId;
        this.email = email;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
