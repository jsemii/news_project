CREATE TABLE IF NOT EXISTS news (
    id           BIGSERIAL PRIMARY KEY,
    url          VARCHAR(2048) NOT NULL UNIQUE,
    title        VARCHAR(500)  NOT NULL,
    source       VARCHAR(100)  NOT NULL,
    published_at TIMESTAMP,
    collected_at TIMESTAMP     NOT NULL DEFAULT now()
);

-- description 컬럼 이력: 한때 크롤링한 기사 본문 전체(또는 RSS 짧은 요약)를 이 컬럼에
-- 저장했었는데, 원문을 DB에 영구 저장하는 것 자체가 저작권(복제권) 리스크가 있다고 판단해
-- 완전히 제거했다(2026-08-19, 수동 마이그레이션 1회 적용 — 이 프로젝트는 Flyway 없이
-- schema.sql 하나로 "지금 원하는 최종 스키마"만 유지하는 방식이라, 컬럼 삭제 같은 1회성
-- 변경은 DB에 직접 적용하고 이 파일은 항상 최종 형태만 남긴다). 이제 원문은 AI 구조화
-- 단계(ai 패키지)가 필요할 때마다 원문 URL을 직접 크롤링해서 메모리에서만 쓰고 바로
-- 버리며, 어떤 테이블에도 저장하지 않는다. 자세한 경위는 docs/troubleshooting.md 참고.

-- news_analysis: 뉴스 1건당 1행. AI가 만든 "공통 정보"(직무와 무관하게 누구에게나 보여줄
-- 요약)를 담는다. "일반 모드"(직무를 아직 안 고른 사용자)에게는 이 테이블 내용만 보여준다.
CREATE TABLE IF NOT EXISTS news_analysis (
    id        BIGSERIAL PRIMARY KEY,
    news_id   BIGINT NOT NULL UNIQUE REFERENCES news(id),
    summary   TEXT NOT NULL,
    analyzed_at TIMESTAMP NOT NULL DEFAULT now()
);

-- importance_score: "이 뉴스가 IT전산/데이터분석/백엔드 취준생에게 얼마나 중요한지"를
-- AI가 요약/직무별 분석과 같은 호출에서 함께 매긴 1~10점 점수다(추가 LLM 호출 없음).
-- 브리핑 조회 시 이 점수가 높은 순으로 정렬해서 상위 N건만 보여주는 데 쓰인다.
-- 판단 이유(reason)는 로그로만 남기고 DB에는 저장하지 않는다.
ALTER TABLE news_analysis ADD COLUMN IF NOT EXISTS importance_score INT NOT NULL DEFAULT 0;

-- news_industry: 뉴스 1건이 여러 산업과 관련될 수 있어서(다중 선택), "뉴스 1건 : 산업 여러 개"
-- 관계를 콤마 구분 문자열이 아니라 정규화된 테이블로 표현한다. 한 뉴스에 같은 산업이
-- 두 번 태깅되지 않도록 UNIQUE(news_id, industry)로 막는다.
CREATE TABLE IF NOT EXISTS news_industry (
    id       BIGSERIAL PRIMARY KEY,
    news_id  BIGINT NOT NULL REFERENCES news(id),
    industry VARCHAR(50) NOT NULL,
    UNIQUE (news_id, industry)
);

-- news_job_analysis: 뉴스 1건마다 직무(IT전산/데이터분석/백엔드) 각각의 관점으로 재해석한
-- 내용을 담는다. 산업처럼 "관련 있는 직무만 선택"이 아니라, 정해진 3개 직무 전부에 대해
-- 항상 분석을 만든다 — 그래서 뉴스 1건당 정확히 3행이 쌓인다. 같은 뉴스·같은 직무 조합이
-- 중복 저장되지 않도록 UNIQUE(news_id, job)로 막는다.
CREATE TABLE IF NOT EXISTS news_job_analysis (
    id              BIGSERIAL PRIMARY KEY,
    news_id         BIGINT NOT NULL REFERENCES news(id),
    job             VARCHAR(30) NOT NULL,
    why_it_matters  TEXT NOT NULL,
    key_skills      TEXT NOT NULL,
    UNIQUE (news_id, job)
);

-- news_filtered_out: NewsRelevanceFilter(규칙 기반 1차 필터)가 "AI로 분석할 가치가 없다"고
-- 판단해서 건너뛴 뉴스를 기록한다. 이 표시가 없으면, 필터링된 뉴스는 news_analysis에도
-- 행이 안 생기기 때문에 "아직 분석 안 된 뉴스" 조회(selectUnanalyzedNews)에서 매번 다시
-- 걸려서 영원히 같은 뉴스를 반복해서 재검토하게 된다(실제로 겪은 문제 — docs/troubleshooting.md
-- 6번 참고). news_analysis와 이 테이블 중 "둘 중 하나라도 있으면" 처리 완료로 취급한다.
CREATE TABLE IF NOT EXISTS news_filtered_out (
    news_id     BIGINT PRIMARY KEY REFERENCES news(id),
    filtered_at TIMESTAMP NOT NULL DEFAULT now()
);
