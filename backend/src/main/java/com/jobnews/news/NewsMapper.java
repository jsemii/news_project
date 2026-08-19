package com.jobnews.news;

import org.apache.ibatis.annotations.Mapper;

/**
 * [전체 흐름에서의 위치] "저장" 단계에서 실제로 DB와 대화하는 창구 역할의 인터페이스입니다.
 * 이 인터페이스 자체에는 로직(코드)이 없고, 메서드 이름과 실제 SQL문을 연결해주는
 * NewsMapper.xml(resources/mapper 폴더)이 진짜 구현체 역할을 합니다.
 * 즉 "이 메서드를 부르면 이런 SQL이 실행된다"는 약속만 여기 적혀 있습니다.
 */
// @Mapper: 이 인터페이스가 MyBatis 매퍼(=SQL 실행 창구)라는 표시입니다.
// config 패키지의 @MapperScan과 함께 있어야, 스프링이 이 인터페이스의 실제 구현체를
// 자동으로 만들어서 NewsService 같은 곳에 주입해줍니다. 안 쓰면 이 인터페이스는
// 그냥 빈 약속일 뿐, 실제로 동작하는 객체가 만들어지지 않습니다.
@Mapper
public interface NewsMapper {

    // [무엇을 받아서] 기사의 url 문자열 하나를 받습니다.
    // [무엇을 하고] "URL 중복 제거" 로직의 핵심 조회입니다. NewsMapper.xml에 있는
    //              SELECT EXISTS(...) 쿼리가 실행되어, 같은 url을 가진 행이 이미
    //              news 테이블에 있는지 확인합니다.
    // [무엇을 돌려주는지] 이미 있으면 true, 없으면(=신규 기사) false를 돌려줍니다.
    boolean existsByUrl(String url);

    // [무엇을 받아서] 저장할 News 객체 하나를 받습니다.
    // [무엇을 하고] NewsMapper.xml의 INSERT 쿼리가 실행되어 news 테이블에 새 행이 추가됩니다.
    // [무엇을 돌려주는지] 영향을 받은 행(row)의 개수를 돌려줍니다. 보통 성공하면 1입니다.
    int insert(News news);
}
