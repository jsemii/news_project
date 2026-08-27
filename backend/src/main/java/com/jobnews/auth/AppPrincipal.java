package com.jobnews.auth;

/**
 * [전체 흐름에서의 위치] GitHub 로그인(OAuth2User)과 Google 로그인(OidcUser)은 Spring
 * Security에서 서로 다른 타입이라, 로그인 이후 어느 쪽으로 들어왔든 우리 자신의 User
 * 정보(id/email/name/role)를 똑같은 방법으로 꺼낼 수 있게 해주는 공통 인터페이스입니다.
 * AppOAuth2User/AppOidcUser 둘 다 이 인터페이스를 구현합니다. AuthController가
 * "principal instanceof AppPrincipal"만 확인하면 provider 종류를 몰라도 됩니다.
 */
public interface AppPrincipal {

    User getAppUser();
}
