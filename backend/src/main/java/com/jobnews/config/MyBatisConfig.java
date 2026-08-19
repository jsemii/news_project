package com.jobnews.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * [전체 흐름에서의 위치] "저장" 단계를 위한 기반 설비를 준비하는 설정 클래스입니다.
 * MyBatis(자바 코드와 SQL문을 연결해주는 라이브러리)가 동작하려면 두 가지가 필요합니다.
 * 1) DB 접속 정보를 가진 SqlSessionFactory(SQL을 실행할 세션을 만들어주는 공장) 객체
 * 2) @Mapper가 붙은 인터페이스(예: NewsMapper)를 스프링이 찾아서 등록해주는 스캔 기능
 * 이 클래스가 그 두 가지를 모두 준비합니다. (docs/troubleshooting.md 2번 항목 참고:
 * 원래는 별도 설정 없이 자동으로 되어야 하지만, 이 프로젝트의 Spring Boot 버전에서는
 * 자동 설정이 제대로 동작하지 않아 아래처럼 직접 만들어줬습니다.)
 */
@Configuration
// @MapperScan("com.jobnews"): com.jobnews 패키지 밑에서 @Mapper가 붙은 인터페이스
// (예: NewsMapper)를 전부 찾아서 "이 인터페이스를 실제로 구현한 객체"를 스프링이 자동으로
// 만들어 등록하게 하는 어노테이션입니다. 안 쓰면 NewsMapper 같은 인터페이스는 구현체가
// 없어서 다른 클래스(NewsService 등)에 주입될 수 없고, 앱 실행 시 에러가 납니다.
@MapperScan("com.jobnews")
public class MyBatisConfig {

    // [무엇을 받아서] 스프링이 이미 만들어둔 DataSource(DB 접속 정보를 가진 객체)를 받습니다.
    // [무엇을 하고] MyBatis가 SQL을 실행할 때 쓰는 SqlSessionFactory를 직접 만듭니다.
    //              이때 DB 접속 정보(dataSource)와, SQL문이 적힌 XML 파일들의 위치
    //              (resources/mapper/*.xml)를 함께 지정해줍니다.
    // [무엇을 돌려주는지] 완성된 SqlSessionFactory 객체를 돌려줍니다.
    // [왜 필요한지] 원래는 mybatis-spring-boot-starter 라이브러리가 이 객체를 자동으로
    //              만들어주는데, 이 프로젝트의 Spring Boot 버전에서는 DataSource보다
    //              MyBatis 자동 설정이 먼저 평가되는 순서 문제 때문에 자동 생성이 실패했습니다.
    //              그래서 "자동으로 될 것"에 기대지 않고 직접 만들어서 문제를 우회했습니다.
    // @Bean: 이 메서드가 반환하는 SqlSessionFactory 객체를 스프링 컨테이너에 등록합니다.
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        // SqlSessionFactoryBean: SqlSessionFactory를 조립해주는 "빌더" 역할의 객체입니다.
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        // classpath:mapper/*.xml → resources/mapper 폴더 안의 모든 XML 매퍼 파일(예: NewsMapper.xml)을
        // 전부 읽어들이라는 뜻입니다. 새 매퍼 XML 파일을 추가해도 이 설정을 또 바꿀 필요가 없습니다.
        factoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath:mapper/*.xml"));
        return factoryBean.getObject();
    }
}
