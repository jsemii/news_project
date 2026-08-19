CREATE TABLE IF NOT EXISTS news (
    id           BIGSERIAL PRIMARY KEY,
    url          VARCHAR(2048) NOT NULL UNIQUE,
    title        VARCHAR(500)  NOT NULL,
    description  TEXT,
    source       VARCHAR(100)  NOT NULL,
    published_at TIMESTAMP,
    collected_at TIMESTAMP     NOT NULL DEFAULT now()
);

-- description 컬럼 참고: 한때 크롤링한 기사 본문을 담을 컬럼을 content_raw라는 별도 이름으로
-- 만들었었는데, description(원래는 RSS 짧은 요약용)과 굳이 나눌 필요가 없다고 판단해
-- content_raw의 값을 description으로 합치고 content_raw는 없앴다(2026-08-19, 수동 마이그레이션
-- 1회 적용 — 이 프로젝트는 Flyway 같은 마이그레이션 도구 없이 schema.sql 하나로 "지금 원하는
-- 최종 스키마"만 유지하기 때문에, 컬럼 이름을 바꾸는 것 같은 1회성 변경은 DB에 직접 적용하고
-- 이 파일은 항상 최종 형태만 남긴다).
-- description은 이제 크롤링 성공 시 기사 본문 전체, 실패 시 NULL이 들어간다. LLM(AI 구조화
-- 단계)의 입력 재료로만 쓰는 내부용 컬럼이며, API 응답에는 절대 그대로 노출하지 않는다
-- (저작권 문제 방지 목적).
