package com.jobnews.auth;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * [전체 흐름에서의 위치] 로그인 사용자 계정(users 테이블)과 대화하는 창구입니다.
 * UserProvisioningService가 "이미 있으면 재사용, 없으면 새로 가입"할 때 이 두 메서드만
 * 있으면 됩니다 — 조회/조회 API(관리자 대시보드 등)는 다음 작업 범위입니다.
 */
@Mapper
public interface UserMapper {

    // [무엇을 받아서] OAuth provider 이름("github"/"google")과 그 provider가 발급한
    //              고유 ID를 받습니다.
    // [무엇을 하고] users 테이블에서 이 조합으로 이미 가입된 계정이 있는지 찾습니다.
    // [무엇을 돌려주는지] 있으면 User, 없으면 null(첫 로그인이라는 뜻 — 에러 아님,
    //              UserProvisioningService가 이 경우 insertUser를 호출합니다).
    User selectByProviderAndProviderId(@Param("provider") String provider,
                                        @Param("providerId") String providerId);

    // [무엇을 받아서] 새로 가입하는 사용자(User, id는 아직 없음)를 받습니다.
    // [무엇을 하고] users 테이블에 1행을 추가합니다. role은 DB DEFAULT('USER')로,
    //              id/created_at도 DB가 채웁니다.
    // [무엇을 돌려주는지] 반환값은 크게 의미 없음 — 대신 useGeneratedKeys로 이 호출이
    //              끝나면 매개변수로 넘긴 User 객체의 id 필드가 실제 생성된 값으로
    //              채워집니다(JWT에 즉시 담아야 하므로 곧바로 필요합니다).
    int insertUser(User user);
}
