# backend (Java 21 / Spring Boot)

뉴스 수집 → AI 구조화 → 직무별 브리핑 생성까지의 파이프라인을 담당.

## 패키지 구조 (`src/main/java/com/jobnews/`)

| 패키지 | 역할 |
|---|---|
| `collector` | RSS 파서 + WebClient + `@Scheduled` 기반 뉴스 수집/원본 저장 |
| `ai` | 수집된 뉴스를 산업/직무 태그, 요약 등으로 구조화 (LLM 연동) |
| `news` | 뉴스 원본/구조화 데이터 도메인 (entity, mapper) |
| `industry` | 산업 도메인 (관세/반도체/AI/금리/고용 등 분류 체계) |
| `job` | 직무 도메인 (사용자가 선택하는 IT 직무 목록) |
| `briefing` | 산업 변화 ↔ 직무 영향을 연결한 맞춤 브리핑 생성/조회 |
| `common` | 공통 예외 처리, 응답 포맷, 유틸 |
| `config` | WebClient, Scheduler, MyBatis, CORS 등 설정 |

## 리소스 구조 (`src/main/resources/`)

- `mapper/` — MyBatis XML 매퍼
- `db/migration/` — DDL / 스키마 초기화 스크립트
- `application.yml` (추후 추가)

> 아직 구현 전 단계 — 패키지 구조만 준비된 상태. 빌드 파일(build.gradle/pom.xml)도 추후 추가.
