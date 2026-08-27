package com.jobnews.auth;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * [전체 흐름에서의 위치] Google 로그인(OIDC) 처리를 담당합니다. Google은 OpenID
 * Connect(OIDC)를 쓰기 때문에 GitHub와 다른 훅(OidcUserService)을 확장해야 합니다.
 * Google이 내려주는 표준 클레임(sub/email/name)을 꺼내 UserProvisioningService로
 * "찾거나 새로 가입"하는 로직은 CustomOAuth2UserService(GitHub)와 완전히 같은 방식을
 * 씁니다 — 두 클래스가 서로 다른 것은 provider 응답에서 정보를 꺼내는 부분뿐입니다.
 */
@Service
public class CustomOidcUserService extends OidcUserService {

    private final UserProvisioningService userProvisioningService;

    public CustomOidcUserService(UserProvisioningService userProvisioningService) {
        this.userProvisioningService = userProvisioningService;
    }

    // [무엇을 받아서] Google과의 OIDC 토큰 교환이 끝난 뒤의 요청 정보를 받습니다.
    // [무엇을 하고] 부모 클래스(super.loadUser)가 ID 토큰을 검증하고 필요하면 UserInfo
    //              엔드포인트까지 호출해서 원본 사용자 정보(OidcUser)를 가져옵니다.
    //              OIDC 표준 클레임인 sub(고유 ID)/email/name을 그대로 씁니다(Google은
    //              이 세 값을 항상 내려줍니다 — GitHub의 email null 문제와 다름).
    // [무엇을 돌려주는지] 우리 User 정보와 role 기반 권한을 함께 담은 AppOidcUser.
    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String providerId = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();

        User user = userProvisioningService.findOrCreate("google", providerId, email, name);

        return new AppOidcUser(oidcUser, user);
    }
}
