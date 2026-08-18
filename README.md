# 취준생 맞춤 산업/직무 뉴스 브리핑 서비스

IT 직무는 정했지만 산업(관세/반도체/AI/금리/고용 등)은 아직 정하지 못한 취준생에게,
"이 뉴스가 내 직무에 어떤 영향을 주는지"를 자동으로 연결해서 브리핑해주는 서비스.

## 프로젝트 구조

```
news_project/
├── frontend/           React (SPA) - 브리핑 조회/직무 선택 UI
├── backend/            Java21 + Spring Boot - 수집/구조화/브리핑 API
├── infra/              Docker, Nginx, DB 초기화 스크립트
└── docs/               기획/설계 문서 (ERD, API 명세 등)
```

## 핵심 기능과 코드 위치 매핑

| 기능 | 담당 모듈 |
|---|---|
| 1. 뉴스 수집/저장 파이프라인 | `backend/.../collector`, `backend/.../news` |
| 2. AI 뉴스 구조화 (산업/직무 태깅, 요약) | `backend/.../ai` |
| 3. 직무별 맞춤 브리핑 | `backend/.../briefing`, `backend/.../job` |

## 실행 (추후 작성)

- backend: `./gradlew bootRun` (예정)
- frontend: `npm run dev` (예정)
- infra: `docker compose up -d` (예정)
