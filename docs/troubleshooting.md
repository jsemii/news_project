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

## 5. Jackson 클래스가 com.fasterxml.jackson 패키지에서 컴파일 오류
#### 현상
OpenAI 응답 JSON을 파싱하려고 `import com.fasterxml.jackson.databind.ObjectMapper;`,
`import com.fasterxml.jackson.databind.JsonNode;`를 썼는데, 빌드 시
`package com.fasterxml.jackson.databind does not exist`로 컴파일 자체가 실패했다.

#### 원인
`./gradlew dependencies --configuration compileClasspath`로 실제 의존성 트리를 확인해보니,
이 프로젝트의 Spring Boot 4.1.0 환경은 `spring-boot-starter-jackson`을 통해 **Jackson 3**
(`tools.jackson.core:jackson-databind:3.1.4`)을 쓰고 있었다. Jackson 3에서는 `ObjectMapper`,
`JsonNode` 같은 핵심 클래스의 패키지가 기존 `com.fasterxml.jackson.databind`에서
`tools.jackson.databind`로 바뀌었다(단, `@JsonProperty` 같은 애노테이션은 여전히
`com.fasterxml.jackson.annotation` 패키지에 남아있어서 더 헷갈렸다).

#### 해결
import를 `tools.jackson.databind.ObjectMapper` / `tools.jackson.databind.JsonNode`로 수정했다.
추가로 컴파일 시 `JsonNode.asText()`가 deprecated라는 경고가 떠서, jackson-databind 3.1.4의
클래스 파일을 `javap`으로 직접 열어 대체 메서드가 `asString()`이라는 것을 확인하고 전부 교체했다.

## 6. 규칙 기반 필터에 걸린 뉴스가 "미분석"으로 영원히 남아 무한 반복됨
#### 현상
`openai.batch-size`를 추가한 뒤, 남은 백로그를 다 처리할 때까지 `POST /api/structuring/run`을
반복 호출하는 스크립트를 돌렸는데, `remainingBacklog`가 23에서 전혀 줄지 않고 매번 같은
`{"totalFound":20,"filteredOut":20,"succeeded":0,"failed":0,"remainingBacklog":23}` 응답만
반복해서 찍혔다. 스크립트가 사실상 무한 루프에 빠졌다(다행히 OpenAI 호출은 발생하지 않아
비용 손실은 없었음).

#### 원인
`NewsRelevanceFilter`가 "분석할 가치 없음"으로 판단한 뉴스는 OpenAI를 호출하지 않고
건너뛰기만 할 뿐, DB 어디에도 "이미 검토했음" 표시를 남기지 않았다. `selectUnanalyzedNews`는
`news_analysis`에 행이 없는 뉴스를 "미분석"으로 판단하는데, 필터링된 뉴스는 애초에
`news_analysis`에 행이 생기지 않으므로 매 배치마다 다시 조회되어 다시 필터링되는 과정이
끝없이 반복됐다. 설계 시점에 "필터링 = 처리 완료"가 아니라 "필터링 = 아직 처리 안 함"으로
잘못 취급한 것이 원인이다.

#### 해결
`news_filtered_out(news_id, filtered_at)` 테이블을 신설했다. `NewsStructuringService`가
뉴스를 필터링할 때 `NewsAnalysisMapper.insertFilteredOut(newsId)`로 표시를 남기고,
`selectUnanalyzedNews`/`countUnanalyzedNews` 쿼리는 `news_analysis`와 `news_filtered_out`
둘 다에 없는 뉴스만 "미분석"으로 취급하도록 수정했다. 회귀 테스트
(`NewsStructuringServiceTest.marksFilteredNewsAsFilteredOutSoItIsNotFetchedAgain`)를 추가하고,
실제 서버로 남은 백로그(총 214건 중 22건 필터링 + 192건 분석)를 끝까지 처리해서
`truly_unprocessed = 0`이 되는 것까지 SQL로 직접 확인했다.

## 7. 크롤링 원문을 DB에 영구 저장하던 구조를 저작권 리스크 완화를 위해 재설계
#### 현상
크롤링한 기사 원문 전체를 `news.description` 컬럼에 영구 저장하고 있었고, LLM이 이
원문을 직접 입력받아 일반 요약과 직무별(IT전산/데이터분석/백엔드) 재해석을 한 번의
호출로 함께 생성했다. 원문을 그대로 DB에 무기한 보관하는 것 자체가 복제권 문제 소지가
있었다.

#### 원인
설계 시점에 "AI 입력 재료는 어딘가에 저장되어 있어야 한다"고 암묵적으로 가정하고,
크롤링(수집 단계) → 저장 → LLM 호출(구조화 단계)을 서로 다른 시점에 분리해서 만들었다.
이 때문에 원문이 두 단계 사이에 DB에 계속 남아있게 됐다.

#### 해결
크롤링을 수집 단계(collector 패키지)에서 완전히 제거하고, AI 구조화 단계(ai 패키지) 안으로
옮겼다 — `NewsStructuringService`가 뉴스 하나를 처리할 때마다 그 자리에서 원문을 크롤링해
지역 변수로만 쓰고, 메서드가 끝나면 버린다(DB에는 어떤 테이블에도 저장하지 않음). LLM
호출도 2단계로 나눴다: 1단계(`OpenAiClient.analyzeGeneral`)는 원문 → 일반 요약/중요도/산업,
2단계(`OpenAiClient.analyzeJobs`)는 **원문이 아니라 1단계 요약**을 입력으로 받아 직무별
재해석을 생성한다. 두 단계 모두 프롬프트에 "원문(또는 요약) 문장을 그대로 옮기지 말고
사실관계만 바탕으로 완전히 새로운 문장을 쓰라"는 지시를 명시했다. `news.description`
컬럼은 더 이상 아무 데도 쓰이지 않아 완전히 삭제했다(기존 214건의 원문 데이터도 컬럼
삭제와 함께 제거됨). 크롤링 실패/원문이 너무 짧은 경우는 `news_filtered_out`으로
처리해서(6번 항목과 같은 이유로 무한 재시도를 방지) 재시도하지 않는다. 실제 OpenAI로
배치(20건, 2단계×20 = 40회 호출)를 돌려 요약·직무별 재해석이 모두 새로운 문장으로
생성되는 것과 `news` 테이블에 `description` 컬럼이 없는 것을 확인했다.

## 8. 요약 길이를 늘리려는 프롬프트 지시가 사실 오류·할루시네이션을 유발
#### 현상
QA 조회(`GET /api/review/summaries`)로 실제 요약 3건을 원문과 직접 대조한 결과, 두 가지
문제를 발견했다.
1. 사실 오류(주체 바꿔치기): "전주시는 법정·의무적 경비를 최우선 반영"인 원문이
   "정부는 필수 경비를 우선 반영"으로 바뀜(행위 주체가 전주시→정부로 잘못 일반화됨).
2. 할루시네이션: 요약 마지막 문장 "이러한 조치는... 향후 다른 지방자치단체에도 영향을
   미칠 가능성이 있다"가 원문 어디에도 없는 내용으로 LLM이 그럴듯하게 지어낸 것이었다.

#### 원인
직전 항목(요약 길이를 400~600자로 늘리는 작업)에서 "맥락/시사점" 요소가 원문에 없어도
"최소 1문장은 반드시 작성하라"고 지시했었다. 이 지시가 사실상 "원문에 없는 시사점을
만들어내라"는 요청과 다름없었고, 그 결과로 할루시네이션이 발생했다. 주체 바꿔치기는
별도로 "정확한 주체를 유지하라"는 명시적 제약이 프롬프트에 전혀 없었기 때문에
발생했다 — 요약 길이/구조에 대한 지시만 있고 사실 정확성에 대한 지시가 없었다.

#### 해결
`OpenAiClient.buildGeneralSystemPrompt()`에 길이보다 우선하는 "정확성 규칙" 2가지를
추가했다: (A) 주체(기관명·지자체명·인명·기업명)를 원문과 다르게 바꾸거나 일반화하지
말 것, (B) 원문에 없는 전망·파급효과·시사점을 추측해서 덧붙이지 말 것. "맥락/시사점"
요소는 원문에 실제로 그런 내용이 있을 때만 쓰도록 바꾸고, 없으면 다른 실제 사실(관계자
발언, 향후 일정 등)로 채우거나 짧게 끝내도록 허용했다(길이 목표보다 정확성이 우선).
프롬프트 마지막에 "주체가 일치하는지, 추측을 덧붙이지 않았는지" 스스로 점검하는 지시도
추가했다. 사용자가 문제를 발견한 두 기사(전주시 재정 재검토, 새만금 현대차 투자 관련
2건)를 실제로 재처리해서, 주체(전주시/이재명 정부/새만금개발청/문성요 청장)가 전부
정확히 유지되고 근거 없는 전망 문장이 더 이상 나오지 않는 것을 직접 확인했다. 트레이드
오프로 요약 평균 길이는 다소 줄어들 수 있음(정확성을 길이보다 우선하기로 한 결정).

## 9. 동시 요청이 겹치면 DuplicateKeyException으로 배치 전체가 500 에러로 죽음
#### 현상
수정된 프롬프트로 기존 318건을 재처리하는 배치를 반복 호출하던 중, 정상적으로 6개
배치(120건)를 처리하고 나서 갑자기 `POST /api/structuring/run`이 500 에러를 냈다.
로그에는 `org.postgresql.util.PSQLException: ERROR: duplicate key value violates unique
constraint "news_analysis_news_id_key" Detail: Key (news_id)=(176) already exists.`가
찍혀 있었다. 이후 반복 스크립트가 같은 500 에러를 계속 반환받으면서도 멈추지 않고
19번 더 헛되이 재시도했다(별도 원인, 아래 "추가로 발견한 문제" 참고).

#### 원인
`news_id=176`의 분석 결과가 13:02:10에 정상 저장된 직후, 같은 뉴스에 대한 두 번째
저장 시도가 뒤이어 들어와 `news_analysis.news_id`의 UNIQUE 제약에 걸려 실패했다.
두 요청이 같은 뉴스를 동시에 "미분석"으로 조회해서 동시에 처리했다는 뜻이다 —
자동화된 재처리 반복 스크립트와, 별도로 Swagger UI에서 수동으로 `/api/structuring/run`을
직접 눌러본 시도가 우연히 겹친 것으로 추정된다. `NewsStructuringService.structureOne()`은
이런 동시 저장 충돌 가능성을 전혀 고려하지 않고 `newsAnalysisSaver.save()`가 던지는
예외를 그대로 상위로 전파시켰고, 그 결과 컨트롤러까지 예외가 튀어나가 HTTP 요청
전체가 500으로 죽으면서 같은 배치에 포함된 나머지 뉴스들까지 처리되지 못하고 버려졌다.

추가로 발견한 문제: 재처리 반복 bash 스크립트가 응답을 `JSON.parse(resp).remainingBacklog`로
파싱했는데, 500 에러 응답에는 `remainingBacklog` 필드가 없어 결과가 `undefined`였다.
`console.log(undefined)`가 문자열 `"undefined"`를 출력하다 보니 `[ -z "$remaining" ]`
검사를 통과하지 못했고(빈 문자열이 아니므로), 뒤이은 `[ "$remaining" -eq 0 ]` 정수
비교에서 `integer expected` 오류만 내고 루프를 멈추지는 못해 같은 에러를 19번 더
반복했다.

#### 해결
`NewsStructuringService.structureOne()`에서 `newsAnalysisSaver.save(...)` 호출을
`try/catch (DuplicateKeyException e)`로 감쌌다. 중복 키 충돌이 나면 "다른 요청이
이미 이 뉴스를 분석해서 저장했다"는 뜻으로 받아들여 경고 로그만 남기고
`StructureOutcome.SUCCEEDED`를 반환하도록 해서, 하나의 충돌이 배치 전체나 HTTP 요청을
죽이지 않고 나머지 뉴스는 계속 처리되도록 고쳤다. 반복 스크립트도 응답에 `"error"`
키가 있는지 먼저 확인하고, `remainingBacklog`가 숫자로 정상 파싱되지 않으면 즉시
멈추도록 고쳤다. 수정 후 실제로 남은 백로그 228건을 12개 배치로 끝까지 재처리해서
단 한 번의 오류 없이 `remainingBacklog: 0`까지 도달하는 것을 확인했고, 처음 문제가
발견됐던 3건(news_id 343, 360, 361)의 DB 저장 요약을 직접 조회해서 주체 바꿔치기나
할루시네이션 없이 정확하게 재처리된 것도 함께 확인했다.

## 10. DB 접속 주소에 localhost가 하드코딩돼 있어 Docker 배포 시 접속 실패할 뻔함
#### 현상
AWS EC2 배포용 Docker 구성(백엔드 Dockerfile, Nginx, docker-compose 확장)을 설계하면서
`backend/src/main/resources/application.yml`을 확인한 결과, DB 접속 URL이
`jdbc:postgresql://localhost:5432/news_briefing`로 호스트가 고정돼 있었다. 지금까지는
문제가 드러나지 않았는데, 로컬 개발에서는 Postgres 컨테이너가 호스트 포트 5432로
노출돼 있어 `localhost`로도 접속이 됐기 때문이다.

#### 원인
설계 시점에 "백엔드도 컨테이너로 띄운다"는 시나리오를 고려하지 않고 로컬 개발 환경
기준으로만 접속 주소를 정했다. Docker Compose로 배포하면 backend 컨테이너 안에서
`localhost`는 backend 컨테이너 자기 자신을 가리키므로(Postgres가 아님), 이 상태로
배포했다면 backend가 기동 시점에 자기 자신의 5432 포트로 접속을 시도하다가 무조건
실패했을 것이다 — 실제로 배포해보기 전에 설계 단계에서 발견해 미리 고쳤다.

#### 해결
`application.yml`의 접속 URL을 `jdbc:postgresql://${DB_HOST:localhost}:5432/news_briefing`로
바꿨다. `DB_HOST` 환경변수가 없으면 지금까지처럼 `localhost`를 기본값으로 쓰고(로컬
`./gradlew bootRun` 흐름은 전혀 안 바뀜), `infra/docker/docker-compose.yml`의 backend
서비스에만 `DB_HOST=db`를 넣어서 Docker 내부 네트워크의 `db` 서비스 이름으로 접속하도록
분기했다. 변경 후 로컬에서 `./gradlew bootRun`으로 정상 접속되는 것과, `docker compose
up --build`로 db→backend→nginx가 헬스체크를 통과하며 순서대로 뜨고 `curl
http://localhost/api/briefings`가 실제 데이터를 반환하는 것을 모두 직접 확인했다.

## 11. EC2에서 backend Docker 이미지 빌드 시 gradlew Permission denied(exit 126)
#### 현상
로컬(Windows, Docker Desktop)에서는 `backend/Dockerfile` 빌드가 문제없이 됐는데,
EC2(Ubuntu, 리눅스)에서 같은 이미지를 빌드하니 `RUN ./gradlew --version` 단계에서
`Permission denied`와 함께 exit code 126으로 실패했다.

#### 원인
`gradlew`는 실행 권한(+x)이 있어야 `./gradlew`로 직접 실행할 수 있는 셸 스크립트다.
이 실행 권한 비트가 리포지토리를 clone/이미지로 COPY하는 과정 어딘가에서 보존되지
않아, EC2의 빌드 환경에서는 실행 권한이 없는 상태로 파일이 들어갔다(로컬 Windows
Docker Desktop 환경에서는 우연히 문제가 드러나지 않았다 — 파일시스템/COPY 처리
방식 차이로 추정). `COPY gradlew ./` 직후 바로 `RUN ./gradlew --version`을 실행해서
문제가 즉시 드러났다.

#### 해결
`backend/Dockerfile`에서 `COPY gradlew ./` 바로 다음 줄에 `RUN chmod +x ./gradlew`를
추가했다. 로컬 파일시스템에 있는 원본 `gradlew`의 실행 권한 상태가 어떻든(Windows에서
clone했든, git이 권한을 어떻게 저장했든) 이미지 안에서 매번 명시적으로 실행 권한을
부여하므로, 빌드 환경(OS)에 상관없이 항상 안전하게 빌드된다. 수정 후 로컬에서
`docker build --no-cache`로 처음부터 다시 빌드해 `./gradlew --version`과
`./gradlew bootJar -x test`가 모두 정상 실행되는 것을 확인했다.

## 12. db 서비스의 5432 포트를 막으면 로컬 ./gradlew bootRun 개발 방식이 깨짐
#### 현상
"backend/db는 nginx를 통해서만 외부에 노출한다"는 배포 원칙에 맞춰
`infra/docker/docker-compose.yml`의 `db` 서비스에서 `ports: ["5432:5432"]`를
제거했다. 이 상태로는 컨테이너로 띄운 `backend` 서비스는 Docker 내부 네트워크로
`db`에 정상 접속되지만, `./gradlew bootRun`으로 호스트에서 직접 실행하는 지금까지의
로컬 개발 방식은 `localhost:5432`로 접속을 시도하다가 실패하게 된다 — 컨테이너 밖
프로세스는 호스트에 열린 포트가 없으면 컨테이너 안의 Postgres에 닿을 방법이 없기
때문이다.

#### 원인
배포 서버(EC2) 기준으로는 5432를 외부에 열 이유가 전혀 없어서 뺀 게 맞지만, 지금까지
이 프로젝트의 로컬 개발은 "Postgres만 컨테이너, 백엔드/프론트는 호스트에서 직접 실행"
방식이었다는 점을 그 자리에서 함께 고려하지 못했다. 배포 설정과 로컬 개발 설정이
같은 `docker-compose.yml` 파일 하나에 섞여 있어서 생긴 문제다.

#### 해결
Docker Compose의 표준 기능인 `docker-compose.override.yml`을 활용했다. 같은 폴더에
이 파일이 있으면 `docker compose up`을 옵션 없이 실행해도 자동으로 원본
`docker-compose.yml`에 덧붙여 적용된다. `infra/docker/docker-compose.override.yml`
(db 서비스에 5432 포트만 다시 열어주는 내용)을 만들고, `.gitignore`에 등록해서 git에는
절대 올라가지 않게 했다 — 배포 서버는 git pull로 `docker-compose.yml`만 받으므로
이 오버라이드가 적용될 일이 없어 포트가 항상 닫힌 채로 유지되고, 로컬 개발 환경에서만
이 파일을 직접 만들어서 포트를 되살릴 수 있다(`infra/README.md`에 안내함). `docker
compose config`로 로컬(오버라이드 있음)은 5432가 노출되고, `docker compose -f
docker-compose.yml config`(오버라이드 없이, 배포 서버 상황 재현)는 5432가 없는 것을
직접 확인했다. 이후 전체 스택(`docker compose up -d`)을 다시 띄워서 nginx→backend→db
전체 흐름이 여전히 정상 동작하는 것과, 호스트의 `./gradlew bootRun` 백엔드도 끊김 없이
계속 DB에 접속되는 것을 함께 확인했다.

## 13. 날짜 탭 이동 시 데이터가 없는 날에도 직전 날짜의 카드가 그대로 남아 보임
#### 현상
사용자가 "8/21 탭엔 8/20 뉴스가, 8/22 탭엔 8/21 뉴스가, 8/23 탭엔 8/21 뉴스가 보인다"고
보고했다. 확인해보니 앞의 두 개(8/21→8/20, 8/22→8/21)는 정상이었다 — 카드에 표시되는
날짜(`formatDate(item.publishedAt)`)는 "AI가 분석한 날짜"가 아니라 "기사가 원래
발행된 날짜"라서, 수집→분석 사이 시차 때문에 하루 정도 차이 나는 게 자연스럽다.
문제는 세 번째였다: 8/23은 `news_analysis`에 분석된 뉴스가 0건이라 백엔드가 항상 빈
배열을 반환하는데도(직접 curl로 재확인함), 화면에는 8/22의 카드(발행일 8/21로 표시)가
그대로 남아 있었다.

#### 원인
`BriefingPage.jsx`의 `useEffect`가 날짜/직무가 바뀔 때마다 새로 fetch는 하지만, 이전
fetch로 채워진 `briefings` state를 새 요청을 시작하는 시점에 비우지 않았다. 목록을
그리는 `<ul>`도 loading/error 상태와 무관하게 항상 `briefings`를 그대로 map 하고
있어서, 새 요청이 실패하거나(또는 React StrictMode가 개발 모드에서 effect를 두 번
실행하면서 생기는 경쟁 상태) 응답이 늦어지는 사이에는 직전 날짜의 카드가 화면에 계속
남아있게 됐다. 정상적으로 성공 응답(빈 배열)이 오면 결국 지워지긴 하지만, 그 사이
잠깐이라도(또는 어떤 이유로 실패하면 계속) 다른 날짜의 데이터가 현재 탭인 것처럼
보이는 게 문제였다.

#### 해결
`useEffect` 안에서 `fetchBriefings`를 호출하기 직전에 `setBriefings([])`를 추가해서,
날짜나 직무를 바꾸는 순간 이전 목록을 즉시 비우도록 했다. 이제 새 응답이 오기 전까지는
"불러오는 중" 상태만 보이고, 이전 날짜의 카드가 잘못 남아있는 일이 없다. `npm run
build`, `npx oxlint` 통과를 확인했다.

## 14. 날짜 탭에 분석일과 동떨어진 발행일의 오래된 기사가 섞여서 나옴
#### 현상
사용자가 "8/21 브리핑 탭에 8/18~8/20에 발행된 기사가 다 섞여서 나온다"고 지적했다.
확인해보니 `selectTopBriefings`/`selectTopBriefingsByJob`이 `news_analysis.analyzed_at`
(분석된 날짜)로만 걸러서, 기사가 실제로 언제 쓰였는지(`news.published_at`)는 전혀
제한하지 않고 있었다.

#### 원인
과거 백로그(300건 이상)를 프롬프트 수정 후 한꺼번에 재처리했던 이력 때문에(문서
7~9번 항목), `analyzed_at`이 같은 날인 기사들 중에 실제 발행일이 며칠씩 차이 나는
경우가 많이 섞여 있었다. 평소처럼 "그날 나온 뉴스를 그날 분석"하는 정상 운영
상태라면 두 날짜가 거의 항상 붙어있어서 문제가 안 드러났겠지만, 날짜 탭 UI를 쓰면서
이 간극이 눈에 띄게 됐다.

#### 해결
`BriefingMapper.xml`의 두 SELECT 모두에 `AND n.published_at::date >= #{date} - 1`
조건을 추가했다. 분석 날짜(탭에서 고른 날짜) 기준으로 "그날 또는 바로 전날 발행된
기사"만 그 날짜의 브리핑으로 보여주고, 그보다 오래된 기사는 제외한다. 실제 서버로
8/21 탭 조회 시 카드들의 발행일이 8/20~8/21로만 좁혀지는 것, 직무 필터(job=백엔드)와
함께 써도 동일하게 적용되는 것, 데이터 없는 날짜(8/23)는 여전히 빈 배열을 반환하는
것을 모두 확인했다.

## 15. EC2에서 backend 컨테이너가 메모리 부족으로 갑자기 꺼짐(OOM, exit 137)
#### 현상
EC2 서버에 배포한 뒤 처음 30분 정도는 사이트가 잘 떴는데, 시간이 좀 지나니 갑자기
`http://3.37.221.184/`에서 브리핑이 "불러오지 못했습니다 (status: 502)"라고 뜨기
시작했다. 서버(EC2)에 들어가서 `docker compose ps`(지금 켜져 있는 프로그램 목록
보기)로 확인했더니, 프론트(nginx)와 DB는 멀쩡한데 **백엔드(backend)가 아예 목록에서
사라져 있었다.** `docker compose ps -a`(꺼진 것까지 전부 보기)로 다시 보니
`Exited (137)`이라고 나왔다 — 즉, 백엔드 프로그램이 스스로 종료된 게 아니라 뭔가에
의해 강제로 꺼진 상태였다. 로그를 봐도 에러 메시지 하나 없이 그냥 뚝 끊겨 있었다.

#### 원인 (비유로 설명)
컴퓨터에는 프로그램이 작업할 때 쓰는 "책상 공간"에 해당하는 RAM(메모리)이 있다.
이 EC2 서버는 그 책상이 **약 1GB밖에 안 되는 작은 사양**이었는데(`free -h`로 확인),
그 위에 DB(Postgres), 백엔드(자바 프로그램), 프론트 서버(nginx) 세 개를 동시에 올려
놓고 쓰고 있었다. 게다가 "책상이 꽉 찼을 때 잠깐 서랍(디스크)에 물건을 밀어 넣어두는"
역할을 하는 스왑(swap)이라는 안전장치도 전혀 설정돼 있지 않았다(`free -h`에서
Swap이 0이었음).

여기에 자바 프로그램(백엔드)은 기본적으로 "내가 볼 수 있는 메모리의 최대 25%까지는
써도 된다"는 식으로 동작하는데, 미리 상한선을 정해두지 않으면 상황에 따라 꽤 많은
메모리를 잡아먹을 수 있다. 책상이 좁은데 스왑(서랍)도 없는 상태에서 이런 일이
겹치면, 리눅스 운영체제는 "메모리가 진짜로 다 떨어졌다"고 판단하는 순간 **가장
메모리를 많이 쓰는 프로그램을 그냥 강제로 꺼버린다**(이걸 OOM killer라고 부르고,
이때 프로그램이 받는 신호가 SIGKILL, 종료 코드로는 137번이다 — 에러 로그를 남길
틈도 없이 즉시 꺼지기 때문에 로그가 깨끗하게 끊긴 것도 이 때문이다). 우리 백엔드가
바로 그 "가장 많이 쓰는 프로그램"으로 지목돼 꺼진 것이다.

#### 해결
두 가지를 같이 적용했다.
1. **자바 프로그램이 쓸 수 있는 메모리에 명확한 상한선을 그었다.** `backend/Dockerfile`의
   실행 명령을 `java -jar app.jar`에서 `java -Xmx384m -jar app.jar`로 바꿨다
   (`-Xmx384m` = "최대 384MB까지만 써라"는 뜻). 이렇게 하면 자바 프로그램이 필요
   이상으로 욕심을 부려서 다른 프로그램(DB, nginx) 것까지 침범하는 일을 막을 수 있다.
2. **EC2 서버에 스왑(서랍 공간)을 새로 만들어줬다.** 디스크 용량 1GB를 떼서 스왑으로
   등록했다(`swapon`). 이제 메모리가 순간적으로 부족해져도 바로 프로그램을 죽이는
   대신, 먼저 디스크의 여유 공간을 잠깐 빌려 쓰고 넘어갈 수 있다.

수정한 코드를 EC2에 다시 배포(`git pull` 후 `docker compose up --build -d`)하고
스왑도 추가한 뒤, **`http://3.37.221.184/`가 다시 정상적으로 뜨는 것을 실제로
확인했다.** 앞으로도 같은 문제가 반복되면(로그가 갑자기 끊기고 `Exited (137)`이
다시 뜨면), EC2 인스턴스 자체를 메모리가 더 큰 사양으로 바꾸는 것도 고려해야 한다.
