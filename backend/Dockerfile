# ── 1단계: 빌드 스테이지 ─────────────────────────────────────────────
# Gradle Wrapper(./gradlew)로 소스를 컴파일해서 실행 가능한 jar 하나를 만드는 단계입니다.
# JDK(컴파일에 필요, 용량 큼)가 필요한 단계라서, 최종 이미지에는 이 스테이지 자체가 남지
# 않고 결과물(jar 파일)만 다음 스테이지로 옮겨갑니다("멀티스테이지 빌드" — 최종 이미지를
# 작게 유지하기 위한 기법).
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# gradlew와 wrapper 설정만 먼저 복사해서 의존성 캐시 레이어를 만듭니다. build.gradle이
# 안 바뀌었으면 이후 재빌드 때 이 레이어가 재사용되어 빌드가 빨라집니다.
COPY gradlew ./
# chmod +x: git이나 운영체제(특히 Windows에서 clone한 뒤 리눅스 서버로 옮기는 경우)에 따라
# gradlew 파일의 실행 권한이 보존되지 않을 수 있습니다(EC2에서 실제로 "Permission denied"로
# 겪은 문제 — docs/troubleshooting.md 참고). 로컬 파일시스템의 원본 권한 상태를 신뢰하지 않고,
# 이미지 안에서 매번 확실하게 실행 권한을 부여합니다.
RUN chmod +x ./gradlew
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
RUN ./gradlew --version

COPY src ./src

# -x test: 이 빌드 스테이지는 격리된 컨테이너 안이라 실제 Postgres에 접속할 수 없습니다.
# 이 프로젝트의 테스트는 전부 실제 DB 접속이 필요한 통합 테스트라서(문서: docs/troubleshooting.md),
# 여기서 테스트를 돌리면 DB_PASSWORD가 없어서든 DB가 없어서든 무조건 실패합니다. 실제 테스트
# 검증은 지금처럼 로컬 개발 환경(진짜 DB가 있는 곳)에서 ./gradlew build로 따로 수행합니다.
# --no-daemon: 컨테이너는 빌드 한 번 하고 버려지는 일회성 환경이라 데몬을 계속 띄워둘 이유가 없습니다.
RUN ./gradlew bootJar -x test --no-daemon

# ── 2단계: 실행 스테이지 ─────────────────────────────────────────────
# 실제로 서비스가 돌아가는 최종 이미지입니다. jar를 "실행"만 하면 되므로 컴파일 도구가
# 들어있는 무거운 JDK 대신 더 가벼운 JRE(자바 실행 환경)만 사용합니다.
FROM eclipse-temurin:21-jre-alpine

# curl: docker-compose.yml의 healthcheck가 "GET /api/briefings가 200을 주는지"로 이 컨테이너가
# 정상인지 확인하는데, 그 확인 명령이 curl을 씁니다. alpine 기반 이미지엔 기본으로 없어서 설치합니다.
RUN apk add --no-cache curl

# 컨테이너를 root 권한으로 실행하지 않기 위해 별도 사용자를 만듭니다 — 만약 애플리케이션에
# 취약점이 있어 공격당하더라도, root가 아니면 컨테이너 안에서 할 수 있는 일이 제한됩니다.
RUN addgroup -S app && adduser -S app -G app
USER app

WORKDIR /app
# build/libs/*.jar: build.gradle에 jar 이름을 따로 고정하지 않아서 실제 파일명에 버전
# 문자열(예: backend-0.0.1-SNAPSHOT.jar)이 들어갑니다. 버전이 바뀌어도 이 COPY 구문이
# 안 깨지도록 와일드카드로 아무 jar나 복사해서 app.jar라는 고정 이름으로 둡니다.
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

# 실제 애플리케이션 구동 명령. DB_HOST/DB_PASSWORD/OPENAI_API_KEY는 이미지 안에 넣지 않고
# docker-compose.yml의 environment로 실행 시점에 주입합니다(비밀값을 이미지에 굽지 않기 위함).
ENTRYPOINT ["java", "-jar", "app.jar"]
