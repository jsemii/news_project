-- daily_highlight: "오늘 한 줄 요약" 기능이 쓰는 테이블입니다. 뉴스 1건당 1행이 아니라
-- 날짜 1일당 1행입니다(그날 importance_score가 높은 뉴스들을 관통하는 공통 흐름을 LLM이
-- 한 문장으로 뽑아낸 결과). briefing_date를 기본키로 둬서, 하루 안에 여러 번 다시
-- 계산해도(오전/오후/저녁 배치마다) 그 날짜의 행 하나만 덮어쓰도록(UPSERT) 합니다.
CREATE TABLE IF NOT EXISTS daily_highlight (
    briefing_date  DATE PRIMARY KEY,
    headline       TEXT NOT NULL,
    based_on_count INT NOT NULL,
    generated_at   TIMESTAMP NOT NULL DEFAULT now()
);
