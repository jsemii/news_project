#!/bin/bash
# [전체 흐름에서의 위치] GitHub Actions(.github/workflows/deploy.yml)가 main에 push될 때마다
# SSH로 EC2에 접속해서 실행하는 실제 배포 절차입니다. 워크플로 YAML에는 "이 스크립트를
# 실행해라"는 한 줄만 두고, 실제 로직은 여기에 모아둡니다. 이렇게 나눠두면 사람이 EC2에서
# 직접 `bash infra/scripts/deploy.sh`로 똑같은 절차를 수동 실행해서 디버깅할 수도 있습니다.
#
# set -e: 중간 명령 중 하나라도 실패하면(0이 아닌 종료 코드) 즉시 스크립트를 중단합니다.
# 이게 없으면 예를 들어 빌드가 실패해도 다음 줄로 계속 넘어가서 "배포 성공"처럼 보이는
# 착각이 생길 수 있습니다(docs/troubleshooting.md 23번 항목에서 겪은 문제).
set -e

# 이 스크립트 파일 위치를 기준으로 infra/docker 폴더로 이동합니다. GitHub Actions가
# 어떤 디렉터리에서 이 스크립트를 실행하든(예: 저장소 루트) 항상 같은 곳에서 docker
# compose 명령이 실행되도록 하기 위함입니다.
cd "$(dirname "$0")/../docker"

echo "=== 빌드 캐시 정리 (디스크 공간 확보) ==="
# docker builder prune: 더 이상 쓰이지 않는 빌드 캐시 레이어를 지웁니다.
# docker image prune: 어떤 컨테이너도 쓰지 않는(dangling) 이미지를 지웁니다.
# 둘 다 -f(force)로 확인 프롬프트 없이 실행하되, docker volume(DB 데이터)은 절대
# 건드리지 않습니다 — 자동 배포마다 디스크가 조금씩 차오르다가 언젠가 다시 공간
# 부족으로 빌드가 조용히 실패하는 사고(docs/troubleshooting.md 23번 항목)를 막기 위함입니다.
docker builder prune -f
docker image prune -f

echo "=== 빌드 + 재기동 ==="
# --build: 코드가 바뀐 이미지를 다시 빌드합니다. -d: 백그라운드로 띄웁니다.
docker compose up --build -d

echo "=== 상태 확인 (10초 대기 후) ==="
# 컨테이너들이 healthcheck를 통과할 시간을 잠깐 줍니다.
sleep 10
docker compose ps

# docker compose ps 출력에 "unhealthy"나 "Exit"(비정상 종료) 문자열이 보이면 배포 실패로
# 간주합니다. 이 부분이 없으면, 빌드/기동이 실패해도 예전 컨테이너가 그대로 떠있어서
# 겉보기엔 "정상 배포"처럼 보이는 착각이 생길 수 있습니다(오늘 실제로 겪은 문제).
if docker compose ps | grep -Eiq "unhealthy|exit"; then
  echo "!!! 배포 실패: 비정상 상태의 컨테이너가 있습니다 !!!"
  # 로그를 GitHub Actions 실행 화면에 그대로 남겨서, 사람이 EC2에 직접 SSH로
  # 들어가지 않아도 Actions 탭에서 바로 원인을 확인할 수 있게 합니다.
  docker compose logs --tail 80
  exit 1
fi

echo "=== 배포 성공 ==="
