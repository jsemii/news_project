-- scraps: 로그인한 사용자가 관심 있는 뉴스를 저장해두는 테이블입니다. 사용자 1명이
-- 같은 뉴스를 두 번 스크랩할 수 없도록 UNIQUE(user_id, news_id)로 막습니다. user_id/
-- news_id 둘 다 각각 users/news 테이블을 참조하는 외래키라, 계정이나 뉴스가 삭제되면
-- (현재는 둘 다 삭제 기능이 없지만) 참조 무결성이 깨지지 않게 REFERENCES로 명시합니다.
CREATE TABLE IF NOT EXISTS scraps (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT    NOT NULL REFERENCES users(id),
    news_id    BIGINT    NOT NULL REFERENCES news(id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (user_id, news_id)
);
