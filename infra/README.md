# infra

배포/실행 환경 관련 설정.

- `docker/` — `docker-compose.yml`(db + backend + nginx 전체 구성), `.env`(비밀값, git에는
  `.env.example`만 커밋됨)
- `nginx/` — `default.conf`: 프론트 정적 파일 서빙 + `/api`로 들어오는 요청을 backend
  컨테이너로 리버스 프록시
- `postgres/` — (아직 미사용) DB 초기화 SQL, 볼륨 관련 설정을 위해 예약해둔 폴더. 현재는
  `backend/src/main/resources/schema.sql`을 Spring Boot가 기동 시점에 직접 실행하는 방식이라
  이 폴더에 파일이 없음.

## Dockerfile 위치
`backend/Dockerfile`, `frontend/Dockerfile`처럼 각 애플리케이션 코드와 같은 폴더에 둔다(이
`infra/` 폴더 밑에 모아두지 않음). 이유: 빌드 컨텍스트(COPY가 참조할 수 있는 범위)가 각 앱
폴더 자체이면 경로가 단순해지고, 어떤 Dockerfile이 어떤 앱을 빌드하는지 위치만 봐도 알 수
있다. 단, `frontend/Dockerfile`은 `infra/nginx/default.conf`도 이미지에 포함시켜야 해서
예외적으로 레포 루트를 빌드 컨텍스트로 사용한다(`docker-compose.yml`의 `nginx` 서비스
설정 참고).

## 배포 방법 (EC2 등)
1. `infra/docker/.env.example`을 참고해 `infra/docker/.env`를 만들고 `DB_PASSWORD`,
   `OPENAI_API_KEY` 실제 값을 채운다(git에 커밋하지 않음).
2. `cd infra/docker && docker compose up --build -d`
3. `db`(헬스체크 통과) → `backend`(헬스체크 통과) → `nginx` 순서로 자동으로 뜬다
   (`docker-compose.yml`의 `depends_on: condition: service_healthy`).
4. `http://<서버 주소>/`로 프론트, `http://<서버 주소>/api/...`로 API에 접근한다(둘 다 같은
   80번 포트 — nginx 하나가 정적 서빙과 API 리버스 프록시를 함께 처리).

로컬 개발(`./gradlew bootRun` / `npm run dev`)은 지금까지처럼 그대로 사용할 수 있다 —
`backend/src/main/resources/application.yml`의 DB 접속 주소가 `DB_HOST` 환경변수 유무에 따라
로컬은 `localhost`, Docker Compose 안에서는 `db`로 자동으로 갈린다.

## db 서비스에 5432 포트가 없는 이유 + 로컬 개발 시 필요한 설정
`docker-compose.yml`의 `db` 서비스는 호스트에 5432 포트를 노출하지 않는다 — backend/db는
nginx를 통해서만 외부에 노출한다는 배포 원칙 때문이다(같은 Docker 네트워크 안에서는 포트
노출 없이도 `db`라는 서비스 이름으로 backend가 정상 접속 가능).

다만 `./gradlew bootRun`으로 백엔드를 컨테이너 밖(호스트)에서 직접 실행하는 로컬 개발
방식은 호스트에서 `localhost:5432`로 접속해야 해서, 이 포트가 열려 있어야 한다. 그래서
로컬에서만 아래 파일을 직접 만들어야 한다(레포에는 없음 — `.gitignore` 참고, 배포 서버에
실수로 올라가면 안 되기 때문):

`infra/docker/docker-compose.override.yml`:
```yaml
services:
  db:
    ports:
      - "5432:5432"
```
같은 폴더에 `docker-compose.override.yml`이 있으면 `docker compose up`(옵션 없이)만
실행해도 자동으로 합쳐져서 적용된다. EC2 등 배포 서버는 git pull로 이 파일을 받지
않으므로 항상 포트가 닫힌 상태로 유지된다.
