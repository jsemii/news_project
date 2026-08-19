# Troubleshooting

## 1. @Mapper만으로는 MyBatis 빈 등록이 안 됨
#### 현상
`NewsService`가 `NewsMapper`를 생성자로 주입받도록 만들었는데, 테스트 실행 시
`NoSuchBeanDefinitionException: No qualifying bean of type 'com.jobnews.news.NewsMapper' available`로
컨텍스트 로딩이 실패했다.

#### 원인
`NewsMapper` 인터페이스에 `@Mapper` 애노테이션만 붙이고 `@MapperScan` 설정을 추가하지 않았다.
`mybatis-spring-boot-starter`가 `@Mapper` 인터페이스를 자동으로 스캔해줄 것으로 예상했으나,
이 프로젝트 환경(Spring Boot 4.1.0)에서는 자동 스캔이 동작하지 않아 매퍼가 스프링 빈으로 등록되지 않았다.

#### 해결
`backend/src/main/java/com/jobnews/config/MyBatisConfig.java`에 `@Configuration` + `@MapperScan("com.jobnews")`를
추가해 `com.jobnews` 하위 패키지의 매퍼 인터페이스를 명시적으로 스캔하도록 했다.

## 2. mybatis-spring-boot-starter 자동 설정이 DataSource 빈을 못 찾음 (Spring Boot 4.1.0 호환성)
#### 현상
`@MapperScan` 추가 후에도 여전히 컨텍스트 로딩이 실패했다.
에러 메시지: `Property 'sqlSessionFactory' or 'sqlSessionTemplate' are required`
(`MapperFactoryBean`이 `SqlSessionFactory`를 주입받지 못함)

#### 원인
`--debug` 옵션으로 자동 설정 평가 리포트를 확인한 결과,
`MybatisAutoConfiguration`의 `@ConditionalOnSingleCandidate(DataSource.class)` 조건이
`did not find any beans`로 매칭 실패했다. 실제로는 `HikariDataSource`가 정상적으로 생성되고 있었으므로,
`MybatisAutoConfiguration`이 `DataSourceAutoConfiguration`보다 먼저 평가되는 순서 문제로 판단된다.
`mybatis-spring-boot-starter:3.0.4`는 Spring Boot 3.x 기준으로 만들어져 있어,
Spring Boot 4.1.0의 자동 설정 처리 순서 변경과 호환되지 않는 것으로 보인다.

#### 해결
자동 설정에 의존하지 않고, `MyBatisConfig`에 `SqlSessionFactory` 빈을 직접 등록했다.
`SqlSessionFactoryBean`에 `DataSource`와 `classpath:mapper/*.xml` 매퍼 위치를 수동으로 지정하고,
더 이상 쓰이지 않는 `application.yml`의 `mybatis.*` 자동 설정 프로퍼티는 제거했다.
