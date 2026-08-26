# infra

배포/실행 환경 관련 설정.

- `docker/` — `docker-compose.yml`(db + backend + nginx 전체 구성), `.env`(비밀값, git에는
  `.env.example`만 커밋됨)
- `nginx/` — `default.conf`: 프론트 정적 파일 서빙 + `/api`로 들어오는 요청을 backend
  컨테이너로 리버스 프록시
- `postgres/` — (아직 미사용) 볼륨 관련 설정 등을 위해 예약해둔 폴더. DB 스키마 관리는
  Flyway가 담당하므로(아래 참고) 여기엔 파일이 없음.

## DB 스키마 관리 (Flyway)
DB 스키마 변경 이력은 `backend/src/main/resources/db/migration/`의 `V1__baseline.sql`,
`V2__...sql`처럼 버전이 매겨진 SQL 파일로 관리한다. 앱이 뜰 때마다 Flyway가 아직 적용 안 된
파일을 자동으로 실행한다 — 로컬/EC2 어느 쪽이든 코드를 받아서 재기동하기만 하면 스키마도
같이 맞춰진다(예전처럼 사람이 `psql`로 직접 `ALTER TABLE`을 실행할 필요 없음). `V1__baseline.sql`은
Flyway 도입 전까지 `schema.sql` 하나로 관리하던 스키마 전체를 그대로 옮긴 baseline이라 이후
절대 수정하지 않는다 — 새 스키마 변경은 항상 새 버전 파일(`V2__...`)을 추가한다. 자세한 배경은
`docs/troubleshooting.md`(Flyway 도입 항목) 참고.

## Dockerfile 위치
`backend/Dockerfile`, `frontend/Dockerfile`처럼 각 애플리케이션 코드와 같은 폴더에 둔다(이
`infra/` 폴더 밑에 모아두지 않음). 이유: 빌드 컨텍스트(COPY가 참조할 수 있는 범위)가 각 앱
폴더 자체이면 경로가 단순해지고, 어떤 Dockerfile이 어떤 앱을 빌드하는지 위치만 봐도 알 수
있다. 단, `frontend/Dockerfile`은 `infra/nginx/default.conf`도 이미지에 포함시켜야 해서
예외적으로 레포 루트를 빌드 컨텍스트로 사용한다(`docker-compose.yml`의 `nginx` 서비스
설정 참고).

## 배포 방법 (EC2 등)

### 자동 배포 (GitHub Actions)
`main` 브랜치에 push되면 `.github/workflows/deploy.yml`이 트리거되어, SSH로 EC2에
접속해 `git pull` + `infra/scripts/deploy.sh`(빌드 캐시 정리 → `docker compose
up --build -d` → 컨테이너 상태 확인)를 자동 실행한다. 컨테이너가 `unhealthy`/`Exit`
상태면 스크립트가 로그를 남기고 실패로 끝나서, GitHub 저장소의 Actions 탭에서
바로 실패 여부와 원인 로그를 확인할 수 있다.

필요한 GitHub Secrets(저장소 Settings → Secrets and variables → Actions에 등록):
- `EC2_HOST`, `EC2_USERNAME`, `EC2_SSH_KEY` (SSH 접속용 — `DB_PASSWORD`/
  `OPENAI_API_KEY` 같은 앱 비밀값은 여기 없고 EC2의 `infra/docker/.env`에만 있으며,
  이 워크플로는 그 값들을 전혀 모른다)

EC2 보안 그룹의 22번 포트(SSH)는 GitHub Actions 러너가 매번 다른 IP에서 접속하므로
`0.0.0.0/0`을 허용해야 한다(자세한 배경은 `docs/troubleshooting.md` 24번 항목 참고).
SSH는 키 기반 인증만 허용되므로 노출 범위는 제한적이다.

### 수동 배포 (자동 배포가 안 될 때의 대안)
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

## HTTPS (Let's Encrypt)
`https://newsbriefing.duckdns.org/`로 서비스한다. 인증서는 `certbot/certbot`
공식 이미지로 발급/갱신하며, 호스트에 별도 설치하지 않는다. `docker-compose.yml`의
`certbot` 서비스는 `profiles: ["certbot"]`로 기본 프로필에서 빠져있어서, 평소
배포(`docker compose up --build -d`)에는 안 뜨고 아래처럼 명시적으로 프로필을
지정할 때만 실행된다(그냥 뒀으면 인자 없이 뜨자마자 종료돼 `docker compose ps`에
"Exited"로 잡혀서 `infra/scripts/deploy.sh`의 실패 감지를 오탐시켰을 것이다).

인증서 본체(`certbot-etc` 볼륨, `/etc/letsencrypt`)와 도메인 소유권 확인용
challenge 파일(`certbot-www` 볼륨, `/var/www/certbot`)은 이름 붙은(named) 볼륨이라
`nginx` 컨테이너가 매 배포마다 재생성돼도 그대로 유지된다. `infra/nginx/default.conf`는
80번(HTTP)을 전부 443(HTTPS)으로 리다이렉트하고, `/.well-known/acme-challenge/`만
예외로 80번에 남겨둔다(갱신이 계속 HTTP-01 방식을 쓰기 때문).

최초 발급(도메인이 실제로 이 서버를 가리키게 된 뒤 1회만):
```bash
cd infra/docker
docker compose --profile certbot run --rm certbot certonly \
  --webroot --webroot-path=/var/www/certbot \
  -d newsbriefing.duckdns.org --email <이메일> --agree-tos --no-eff-email
```

자동 갱신은 EC2 호스트의 systemd timer(`certbot-renew.timer`, 하루 2회)가 담당한다.
`docker compose run`은 실행하고 끝나는 명령이라, 이걸 주기적으로 트리거해줄 무언가가
컨테이너 밖(호스트)에 있어야 하기 때문이다. 타이머가 실행하는 내용:
```bash
docker compose --profile certbot run --rm certbot renew --quiet
docker compose exec nginx nginx -s reload
```
`renew`는 만료 30일 이내인 인증서만 실제로 갱신하므로 하루 2회 돌려도 안전하고,
갱신이 없는 날의 `nginx -s reload`도 무중단으로 끝난다. 유닛 파일은 git으로
관리하지 않고 EC2에 직접 만든다(호스트 시스템 설정이라 저장소 파일이 아님).
`systemctl list-timers | grep certbot-renew`로 다음 실행 예정을 확인할 수 있고,
`docker compose --profile certbot run --rm certbot renew --dry-run`으로 실제
발급 없이 갱신 로직만 검증할 수 있다.

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
