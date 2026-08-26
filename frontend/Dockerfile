# 이 Dockerfile은 "레포 루트"를 빌드 컨텍스트로 써야 합니다(infra/nginx/default.conf를
# COPY해야 하는데, 그건 frontend/ 폴더 밖에 있기 때문입니다). docker-compose.yml에서
# `context: ../..`(레포 루트) + `dockerfile: frontend/Dockerfile`로 지정합니다. 그래서
# 아래 COPY 경로들은 전부 "레포 루트 기준" 경로입니다(frontend/... 처럼).

# ── 1단계: 빌드 스테이지 ─────────────────────────────────────────────
# npm run build로 React 소스를 정적 파일(dist/)로 컴파일하는 단계입니다. Node.js가 필요하지만
# 서비스를 실제로 서빙할 땐 전혀 필요 없으므로, 최종 이미지에는 dist/ 결과물만 다음 스테이지로 옮깁니다.
FROM node:22-alpine AS build
WORKDIR /app

# package.json/package-lock.json만 먼저 복사해서 의존성 설치 레이어를 캐싱합니다.
# 소스 코드(frontend/src 등)만 바뀌고 의존성이 안 바뀌었으면, 재빌드 시 npm ci를 다시
# 돌리지 않고 이 레이어를 재사용해서 빌드 속도가 빨라집니다.
COPY frontend/package.json frontend/package-lock.json ./
# npm ci: package-lock.json에 적힌 버전 그대로 정확히 설치합니다(npm install과 달리
# lock 파일을 갱신하지 않아서, "로컬에서 되던 게 배포 환경에서 다른 버전이 깔려서 안 되는"
# 문제를 막아줍니다 — 재현 가능한 빌드에 적합).
RUN npm ci

COPY frontend/ ./
RUN npm run build

# ── 2단계: 실행 스테이지 ─────────────────────────────────────────────
# 빌드된 정적 파일(dist/)을 nginx로 서빙하는 최종 이미지입니다. 이 컨테이너는 프론트 정적
# 서빙 + "/api" 리버스 프록시(설정은 infra/nginx/default.conf)를 함께 담당합니다.
FROM nginx:1.27-alpine

COPY --from=build /app/dist /usr/share/nginx/html
COPY infra/nginx/default.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
EXPOSE 443
