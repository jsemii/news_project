# infra

배포/실행 환경 관련 설정.

- `docker/` — docker-compose.yml, Dockerfile (backend/frontend/postgres)
- `nginx/` — 리버스 프록시 설정 (frontend 정적 서빙 + backend API 라우팅)
- `postgres/` — DB 초기화 SQL, 볼륨 관련 설정

> 아직 구현 전 단계 — 폴더 구조만 준비된 상태.
