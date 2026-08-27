-- users: GitHub/Google OAuth2 로그인으로 생긴 사용자 계정입니다. 별도 회원가입 절차
-- 없이, 처음 로그인하는 순간 이 테이블에 자동으로 1행이 생깁니다(이후 로그인은 기존
-- 행을 그대로 재사용). provider_id는 그 provider(GitHub/Google)가 발급한 고유 ID로,
-- provider와 묶어서(UNIQUE) 같은 사람이 GitHub/Google 둘 다로 로그인해도 서로 다른
-- 계정으로 취급합니다 — 이메일 기준으로 자동 연동하면 계정 탈취 위험이 생길 수 있어서
-- 일부러 하지 않았습니다(필요하면 나중에 별도 "계정 연결" 기능으로 설계).
-- email은 nullable입니다 — GitHub는 사용자가 이메일을 비공개로 설정하면 null이 옵니다.
-- role은 자바 enum(Role)의 이름을 그대로 저장하는 문자열입니다(news_filtered_out.reason과
-- 같은 패턴). 기본값 USER이고, 관리자 지정은 이번 범위에서 DB에 직접 UPDATE하는 방식만
-- 지원합니다(별도 관리 화면은 다음 작업).
CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    provider    VARCHAR(20)  NOT NULL,
    provider_id VARCHAR(100) NOT NULL,
    email       VARCHAR(255),
    name        VARCHAR(100),
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    UNIQUE (provider, provider_id)
);
