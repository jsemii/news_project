package com.jobnews.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [전체 흐름에서의 위치] "첫 로그인이 곧 회원가입"을 실제로 구현하는 곳입니다.
 * GitHub 로그인(CustomOAuth2UserService)과 Google 로그인(CustomOidcUserService)이
 * 서로 다른 provider 응답 형태를 각자 파싱한 뒤, 마지막에 이 서비스를 공통으로 호출해서
 * "이미 가입된 사용자면 재사용, 아니면 새로 가입"을 처리합니다 — 이 로직을 두 서비스에
 * 각각 중복해서 넣지 않기 위해 따로 뺐습니다.
 */
@Service
public class UserProvisioningService {

    private final UserMapper userMapper;

    public UserProvisioningService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    // [무엇을 받아서] OAuth provider 이름("github"/"google"), 그 provider가 발급한 고유
    //              ID, 이메일(없을 수 있음), 이름을 받습니다.
    // [무엇을 하고] provider+providerId로 기존 계정을 찾고, 있으면 그대로 돌려줍니다.
    //              없으면 새로 insert합니다(회원가입). @Transactional인 이유: "있는지
    //              확인 후 없으면 insert"하는 두 단계 사이에 동시에 같은 사용자가 두 번
    //              로그인을 시도하는 극단적인 경우를 대비한 것은 아니고(UNIQUE 제약이
    //              최종 방어선), 두 DB 호출을 하나의 논리적 작업으로 묶기 위함입니다.
    // [무엇을 돌려주는지] role까지 채워진 User. 새로 가입한 경우 insertUser가 id만
    //              채워주므로, DB DEFAULT와 일치하도록 role을 여기서 직접 USER로
    //              세팅해서 돌려줍니다(호출한 쪽이 바로 JWT에 담아 써야 하므로, DB를
    //              다시 조회하지 않고 이 자리에서 완성된 객체를 만들어 줍니다).
    @Transactional
    public User findOrCreate(String provider, String providerId, String email, String name) {
        User existing = userMapper.selectByProviderAndProviderId(provider, providerId);
        if (existing != null) {
            return existing;
        }

        User newUser = new User(provider, providerId, email, name);
        userMapper.insertUser(newUser);
        newUser.setRole(Role.USER);
        return newUser;
    }
}
