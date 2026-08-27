package com.jobnews.auth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * [전체 흐름에서의 위치] GitHub 로그인(비-OIDC OAuth2) 결과를 감싸는 래퍼입니다.
 * CustomOAuth2UserService가 GitHub의 원본 OAuth2User(delegate)와, 그걸로 찾거나
 * 새로 만든 우리 자신의 User(appUser)를 이 클래스로 함께 묶어서 반환합니다.
 * getAttributes()/getName()은 원본(delegate)을 그대로 돌려주고, getAuthorities()만
 * 우리 role(USER/ADMIN) 기반으로 새로 만듭니다 — 이래야 나중에 스프링 시큐리티의
 * hasRole("ADMIN") 같은 검사가 우리 role을 기준으로 동작합니다.
 */
public class AppOAuth2User implements OAuth2User, AppPrincipal {

    private final OAuth2User delegate;
    private final User appUser;

    public AppOAuth2User(OAuth2User delegate, User appUser) {
        this.delegate = delegate;
        this.appUser = appUser;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + appUser.getRole().name()));
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public User getAppUser() {
        return appUser;
    }
}
