package com.jobnews.auth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * [전체 흐름에서의 위치] Google 로그인(OIDC) 결과를 감싸는 래퍼입니다. AppOAuth2User와
 * 같은 목적이지만, Google은 OIDC라서 원본 타입이 OidcUser(claims/idToken/userInfo를
 * 추가로 가짐)라 별도로 만듭니다. getClaims()/getIdToken()/getUserInfo()는 원본
 * (delegate)을 그대로 돌려주고, getAuthorities()만 우리 role 기반으로 새로 만드는 것은
 * AppOAuth2User와 동일합니다.
 */
public class AppOidcUser implements OidcUser, AppPrincipal {

    private final OidcUser delegate;
    private final User appUser;

    public AppOidcUser(OidcUser delegate, User appUser) {
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
    public Map<String, Object> getClaims() {
        return delegate.getClaims();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return delegate.getUserInfo();
    }

    @Override
    public OidcIdToken getIdToken() {
        return delegate.getIdToken();
    }

    @Override
    public User getAppUser() {
        return appUser;
    }
}
