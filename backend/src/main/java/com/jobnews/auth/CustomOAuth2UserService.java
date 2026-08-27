package com.jobnews.auth;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * [전체 흐름에서의 위치] GitHub 로그인(비-OIDC OAuth2) 처리를 담당합니다. Google이
 * OIDC라 별도 훅(CustomOidcUserService)을 쓰는 것과 달리, GitHub는 Spring Security의
 * 일반 OAuth2 로그인 훅(DefaultOAuth2UserService)을 확장합니다. GitHub의 /user
 * 응답을 받아온 뒤, UserProvisioningService로 "찾거나 새로 가입"하고, 결과를
 * AppOAuth2User로 감싸서 돌려줍니다(SecurityConfig가 이 클래스를 .oauth2Login()의
 * userService로 등록합니다).
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserProvisioningService userProvisioningService;

    public CustomOAuth2UserService(UserProvisioningService userProvisioningService) {
        this.userProvisioningService = userProvisioningService;
    }

    // [무엇을 받아서] GitHub와의 OAuth2 토큰 교환이 끝난 뒤의 요청 정보를 받습니다.
    // [무엇을 하고] 부모 클래스(super.loadUser)가 실제로 GitHub의 /user API를 호출해서
    //              원본 사용자 정보(OAuth2User)를 가져옵니다. GitHub 응답의 "id"(숫자
    //              고유 ID), "email"(비공개면 null), "name"(비어있으면 "login"으로
    //              대신함 — GitHub 사용자명은 항상 있음)을 꺼내 findOrCreate를 호출합니다.
    // [무엇을 돌려주는지] 우리 User 정보와 role 기반 권한을 함께 담은 AppOAuth2User.
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String providerId = String.valueOf(oauth2User.getAttributes().get("id"));
        String email = (String) oauth2User.getAttributes().get("email");
        String name = (String) oauth2User.getAttributes().get("name");
        if (name == null || name.isBlank()) {
            name = (String) oauth2User.getAttributes().get("login");
        }

        User user = userProvisioningService.findOrCreate("github", providerId, email, name);

        return new AppOAuth2User(oauth2User, user);
    }
}
