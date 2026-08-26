-- news_job_analysis.importance_score: 뉴스 1건이 특정 직무(IT전산/데이터분석/백엔드)
-- 관점에서 얼마나 중요한지를 나타내는 점수입니다. 기존 news_analysis.importance_score
-- (공통 점수, 3개 직무 전체 기준)와 달리, 이 점수는 오직 이 직무 하나만 놓고 판단합니다
-- (다른 직무와 비교하는 상대평가가 아님 — 프롬프트 원칙 참고). "직무별 브리핑" 탭이
-- 이제 이 점수를 기준으로 그 직무에 진짜 중요한 뉴스를 먼저 보여줍니다.
-- 기존 행은 이 컬럼이 없던 시점에 분석된 것이라 DEFAULT 0으로 채워지고, 이후 재구조화되지
-- 않는 한 실제 값이 들어오지 않습니다(과거 데이터 소급 반영은 이번 범위에 없음).
ALTER TABLE news_job_analysis ADD COLUMN IF NOT EXISTS importance_score INT NOT NULL DEFAULT 0;
