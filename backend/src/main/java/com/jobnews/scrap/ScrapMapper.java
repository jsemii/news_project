package com.jobnews.scrap;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * [전체 흐름에서의 위치] 스크랩 기능이 DB와 대화하는 창구입니다. 스크랩은 단순
 * 추가/삭제/조회뿐이라 별도 Service 계층 없이 ScrapController가 이 Mapper를 직접
 * 호출합니다(briefing 패키지와 같은 관례).
 */
@Mapper
public interface ScrapMapper {

    // [무엇을 받아서] userId/newsId가 채워진 Scrap(아직 id/createdAt은 없음).
    // [무엇을 하고] scraps 테이블에 새 행을 추가합니다. useGeneratedKeys로 DB가
    //              생성한 id가 이 Scrap 객체에 다시 채워집니다(INSERT 직후 별도
    //              조회 없이 바로 응답을 만들 수 있게 하기 위함). 같은 (user_id,
    //              news_id) 조합이 이미 있으면 UNIQUE 제약 위반으로
    //              DuplicateKeyException이 던져집니다 — 이건 ScrapController가
    //              잡아서 "이미 스크랩됨"으로 처리합니다.
    // [무엇을 돌려주는지] 영향받은 행 수(항상 1, 실패하면 예외).
    int insertScrap(Scrap scrap);

    // [무엇을 받아서] 사용자 id와 뉴스 id.
    // [무엇을 하고] 이미 스크랩된 (user_id, news_id) 조합의 기존 행을 찾습니다 —
    //              insertScrap이 중복으로 실패했을 때, 그 기존 행을 그대로
    //              응답으로 돌려주기 위해 씁니다.
    // [무엇을 돌려주는지] 있으면 그 Scrap, 없으면 null.
    Scrap selectByUserAndNews(@Param("userId") Long userId, @Param("newsId") Long newsId);

    // [무엇을 받아서] 사용자 id와 뉴스 id.
    // [무엇을 하고] 그 (user_id, news_id) 조합의 스크랩 행을 삭제합니다. 애초에
    //              스크랩한 적이 없어도(0행 삭제) 에러 없이 그냥 끝납니다 — DELETE는
    //              원래 멱등한 동작이라는 REST 관례를 그대로 따릅니다.
    // [무엇을 돌려주는지] 삭제된 행 수(0 또는 1) — 호출하는 쪽에서 굳이 확인하지
    //              않아도 됩니다.
    int deleteScrap(@Param("userId") Long userId, @Param("newsId") Long newsId);

    // [무엇을 받아서] 사용자 id.
    // [무엇을 하고] 그 사용자의 스크랩 전체를 최신순으로 조회합니다.
    // [무엇을 돌려주는지] 스크랩 목록(비어있으면 빈 리스트 — 에러 아님).
    List<ScrapItem> selectByUser(@Param("userId") Long userId);
}
