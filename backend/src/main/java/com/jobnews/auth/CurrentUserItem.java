package com.jobnews.auth;

/**
 * [전체 흐름에서의 위치] GET /api/auth/me가 프론트엔드에 돌려주는 응답 항목입니다.
 * role까지 함께 내려줘서, 프론트가 이 값으로 관리자 여부를 판단할 수 있게 합니다
 * (요구사항 6 — 실제로 관리자 화면을 막는 로직은 이번 범위가 아니고, role 정보
 * 자체만 우선 내려줍니다).
 */
public class CurrentUserItem {

    private final Long id;
    private final String email;
    private final String name;
    private final String role;

    public CurrentUserItem(Long id, String email, String name, String role) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }
}
