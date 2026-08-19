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

## 3. schema.sql의 DO $$ ... $$ 블록이 Spring 부팅 시 파싱 오류로 실패
#### 현상
`content_raw` 컬럼을 `description`으로 합치는 마이그레이션을 PostgreSQL의 `DO $$ ... END $$;`
블록으로 `schema.sql`에 작성했더니, 앱 기동 시 다음 오류로 컨텍스트 초기화 자체가 실패했다.
`PSQLException: Unterminated dollar quote started at position 3 ... Expected terminating $$`

#### 원인
Spring Boot의 `spring.sql.init.mode: always` 기능은 `schema.sql`을 세미콜론(`;`) 기준으로
단순하게 잘라서 한 문장씩 실행하는데, PostgreSQL의 `DO $$ ... $$` 문법(달러 인용)을 이해하지
못한다. 그래서 `DO $$ BEGIN ... ALTER TABLE news DROP COLUMN description; ALTER TABLE ...`처럼
블록 안에 세미콜론이 여러 개 있으면, 그 블록 전체를 한 문장이 아니라 여러 문장으로 잘못 쪼개서
DB로 보내버려 문법 오류가 난다.

#### 해결
이 프로젝트는 Flyway 같은 정식 마이그레이션 도구 없이 `schema.sql` 하나로 "지금 원하는 최종
스키마"만 유지하는 방식을 쓰기로 했었다(최초 구조 설계 때 결정). 그 방침에 맞춰, 컬럼 이름을
바꾸는 것 같은 1회성 변경은 `schema.sql`에 조건부 로직(DO 블록)으로 넣지 않고, 개발 DB에
`ALTER TABLE news DROP COLUMN description;` → `ALTER TABLE news RENAME COLUMN content_raw TO description;`을
직접 한 번 실행해서 반영했다. `schema.sql`은 DO 블록 없이 최종 형태(`description` 컬럼 하나)만
남기고, 그 위에 왜 이렇게 됐는지 설명하는 주석만 추가했다.

## 4. RSS description을 기사 본문으로 착각 → 원문 크롤링 단계 누락
#### 현상
RSS의 description 필드가 80~100자 수준의 짧은 요약 문장뿐인데, 이를 기사 본문으로 간주하고
AI 구조화(요약/직무 연결)에 사용하려던 상태였음.

#### 원인
설계 시점에 원문 URL을 별도로 크롤링해 본문 전체를 가져오는 단계를 아키텍처에서 누락함.
RSS description을 본문과 동일시한 것이 원인.

#### 해결
Jsoup 기반 본문 크롤링 단계를 파이프라인에 추가(RSS 수집 → 원문 크롤링 → DB 저장 → LLM 분석
순서로 변경). 처음에는 `content_raw` 컬럼을 신설해 원문을 내부 LLM 입력용으로만 저장하고 API로는
노출하지 않도록 설계했고, 이후 별도 컬럼으로 나눌 필요가 없다고 판단해 기존 `description`
컬럼과 합쳤다(3번 항목 참고). "내부 전용, API 노출 금지"라는 원칙은 컬럼명이 바뀐 뒤에도
그대로 유지된다.
