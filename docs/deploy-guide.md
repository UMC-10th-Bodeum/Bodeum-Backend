# 보듬 백엔드 AWS 배포 가이드

`develop` 브랜치를 **EC2 + Docker / RDS MySQL / GHCR / GitHub Actions 자동배포 / Nginx HTTPS** 구성으로 배포한다.

## 아키텍처

```
개발자 push (develop)
      │
      ▼
GitHub Actions ──build──▶ GHCR (ghcr.io/umc-10th-bodeum/bodeum-backend)
      │ SSH
      ▼
   EC2 (Ubuntu)
   ├─ Nginx (443 TLS) ──proxy──▶ 127.0.0.1:8080 (Docker: bodeum-app)
   │                                     │
   └─ docker compose                     ▼
                                    RDS MySQL (3306)
```

- 앱 컨테이너는 `127.0.0.1:8080`에만 바인딩 → 외부에는 Nginx(80/443)만 노출.
- DB는 RDS. 앱은 `.env`의 `DB_URL`로 접속.
- 이미지 태그는 `latest`(+커밋 SHA). EC2는 `latest`를 pull.

리포지토리에 추가된 파일:
- `.github/workflows/deploy.yml` — CI/CD 워크플로우
- `docker-compose.prod.yml` — EC2에서 쓸 compose (앱 컨테이너만)
- `deploy/.env.prod.example` — 운영 환경변수 템플릿
- `deploy/nginx/bodeum.conf` — Nginx 리버스 프록시 설정

---

## 사전 체크리스트

- [ ] EC2(Ubuntu) 인스턴스 + SSH 접속 가능한 키페어(.pem)
- [ ] RDS MySQL 인스턴스 (엔드포인트/마스터 계정/비밀번호)
- [ ] 도메인 (DNS 레코드를 EC2로 지정할 수 있는 관리 권한)
- [ ] GitHub 리포지토리 관리자 권한 (Secrets 등록용)
- [ ] 카카오/네이버 개발자 콘솔 접근 (리다이렉트 URI 등록용)

> 실제 비밀값(RDS 비번, JWT_SECRET, OAuth 시크릿 등)은 이 문서에 적지 말 것. 팀 컨벤션대로 노션 `.env` 페이지와 EC2의 `~/bodeum/.env`에만 둔다.

---

## Part A. RDS MySQL 준비

### A-1. 보안 그룹 (EC2 → RDS 접속 허용)
1. RDS 인스턴스의 보안 그룹(inbound) 편집.
2. 규칙 추가: **Type=MySQL/Aurora(3306)**, **Source=EC2의 보안 그룹**(또는 EC2 프라이빗 IP).
   - 같은 VPC면 프라이빗 통신 권장. 퍼블릭 접속은 지양.
3. RDS는 퍼블릭 액세스 **비활성** 권장(EC2를 통해서만 접근).

### A-2. 데이터베이스 생성
RDS에 `bodeum` 스키마가 없으면 생성한다. EC2에서:
```bash
sudo apt-get update && sudo apt-get install -y mysql-client
mysql -h <RDS_ENDPOINT> -u <MASTER_USER> -p -e "CREATE DATABASE IF NOT EXISTS bodeum CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

### A-3. 스키마 준비 ⚠️ 중요
앱은 `ddl-auto=validate`(dev 프로파일)로 뜨므로 **엔티티와 일치하는 테이블이 미리 있어야** 부팅된다.

- **팀이 이미 스키마를 올려둔 RDS라면** → 그대로 두고 A-4로.
- **새(빈) RDS라면** → 아래 중 하나로 베이스 스키마를 만든다.

  **방법 1 (권장, 가장 단순): `update`로 1회 부트스트랩**
  현재 엔티티가 최종 형태이므로, 한 번 `ddl-auto=update`로 띄우면 전체 테이블이 생성된다.
  EC2 `~/bodeum/`에서 임시로 profile만 바꿔 1회 실행:
  ```bash
  # .env 준비된 상태에서
  docker run --rm --env-file .env -e SPRING_PROFILES_ACTIVE=local \
    ghcr.io/umc-10th-bodeum/bodeum-backend:latest
  # 부팅 로그에 테이블 생성이 끝나면 Ctrl+C 로 종료
  ```
  이후 정상 배포는 dev(validate)로 뜬다. `sql/`의 증분 마이그레이션들은 **기존 구(舊)스키마를 옮기는 용도**라 새 DB에는 적용하지 않는다.

  **방법 2: 팀 개발 DB 스키마 덤프 후 로드**
  ```bash
  mysqldump -h <DEV_DB> -u <u> -p --no-data bodeum > schema.sql
  mysql -h <RDS_ENDPOINT> -u <MASTER_USER> -p bodeum < schema.sql
  ```

> validate 실패 로그(`missing table/column`)가 나오면 스키마와 엔티티 불일치다. 방법 1로 다시 맞추거나 팀에 문의.

### A-4. 접속 확인
```bash
mysql -h <RDS_ENDPOINT> -u <MASTER_USER> -p -e "SHOW DATABASES;"
```

---

## Part B. EC2 준비 (Ubuntu)

SSH 접속: `ssh -i <키>.pem ubuntu@<EC2_PUBLIC_IP>`

### B-1. 보안 그룹 (inbound)
- 22 (SSH) — 본인 IP로 제한 권장
- 80 (HTTP) — 0.0.0.0/0 (certbot 인증 + HTTPS 리다이렉트)
- 443 (HTTPS) — 0.0.0.0/0
- 8080은 **열지 않는다** (앱은 localhost 전용, Nginx 경유).

### B-2. Docker 설치
```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker $USER
newgrp docker   # 또는 재로그인
docker --version && docker compose version
```

### B-3. 배포 디렉터리 구성
EC2의 `~/bodeum/`에 compose와 env를 둔다. 로컬에서 파일을 올린다:
```bash
# 로컬 PC에서 실행 (프로젝트 루트)
scp -i <키>.pem docker-compose.prod.yml ubuntu@<EC2_PUBLIC_IP>:~/bodeum/docker-compose.yml
scp -i <키>.pem deploy/.env.prod.example ubuntu@<EC2_PUBLIC_IP>:~/bodeum/.env
```
그다음 EC2에서 `~/bodeum/.env`를 실제 값으로 채운다:
```bash
mkdir -p ~/bodeum && nano ~/bodeum/.env
```
- `DB_URL`을 RDS 엔드포인트로, `BODEUM_BASE_URL`과 OAuth 리다이렉트를 `https://<도메인>`으로.
- `chmod 600 ~/bodeum/.env` 로 권한 제한.

> compose 파일명은 EC2에서 `docker-compose.yml`이어야 워크플로우의 `docker compose` 명령이 그대로 동작한다.

---

## Part C. 도메인 & HTTPS

### C-1. DNS
도메인 관리 콘솔에서 **A 레코드**를 EC2 퍼블릭 IP로 지정.
- 고정 IP가 필요하면 EC2에 **Elastic IP**를 할당해 두는 것을 권장(재부팅 시 IP 변경 방지).
- 전파 확인: `dig +short <도메인>` 이 EC2 IP를 반환.

### C-2. Nginx + Certbot
```bash
sudo apt-get install -y nginx
# 리포의 deploy/nginx/bodeum.conf 를 올려서 사용 (또는 직접 작성)
scp -i <키>.pem deploy/nginx/bodeum.conf ubuntu@<EC2_PUBLIC_IP>:/tmp/bodeum.conf   # 로컬에서
```
EC2에서:
```bash
sudo mv /tmp/bodeum.conf /etc/nginx/sites-available/bodeum
sudo sed -i 's/<YOUR_DOMAIN>/실제도메인/g' /etc/nginx/sites-available/bodeum
sudo ln -sf /etc/nginx/sites-available/bodeum /etc/nginx/sites-enabled/bodeum
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx

# Let's Encrypt 인증서 발급 (443 블록/리다이렉트 자동 추가)
sudo apt-get install -y certbot python3-certbot-nginx
sudo certbot --nginx -d 실제도메인 --agree-tos -m <관리자이메일> --redirect
```
certbot이 자동 갱신 타이머를 등록한다(`systemctl status certbot.timer`).

---

## Part D. GitHub Secrets 등록

리포지토리 → Settings → Secrets and variables → Actions → New repository secret:

| Secret | 값 |
|---|---|
| `EC2_HOST` | EC2 퍼블릭 IP 또는 도메인 |
| `EC2_USER` | `ubuntu` |
| `EC2_SSH_KEY` | SSH 개인키(.pem) **전체 내용** (`-----BEGIN...` 포함) |
| `EC2_PORT` | `22` (비워도 기본 22) |

- GHCR push/pull은 `GITHUB_TOKEN`(자동 제공)을 쓰므로 별도 토큰 불필요.
- GHCR 패키지는 기본 private. 워크플로우가 `GITHUB_TOKEN`으로 로그인해 pull하므로 그대로 두면 된다.

---

## Part E. OAuth 리다이렉트 URI 갱신

콜백 경로는 `GET /api/v1/auth/callback/{provider}` 이다. 각 콘솔에 등록:
- **카카오** 개발자콘솔 → 카카오 로그인 → Redirect URI:
  `https://<도메인>/api/v1/auth/callback/kakao`
- **네이버** 개발자콘솔 → 서비스 URL/Callback URL:
  `https://<도메인>/api/v1/auth/callback/naver`

그리고 `~/bodeum/.env`의 `KAKAO_REDIRECT_URI` / `NAVER_REDIRECT_URI`를 동일 값으로 맞춘다.

---

## Part F. 첫 배포 & 검증

### F-1. 배포 파일을 develop에 반영
`.github/workflows/deploy.yml` 등은 **GitHub의 develop 브랜치에 있어야** Actions가 실행된다.
현재 `chore/deploy` 브랜치에서 작업했으므로 PR → develop 머지(팀 컨벤션: Squash Merge, 1인 승인) 후 동작한다.

### F-2. 자동 배포 트리거
develop에 push/머지되면 워크플로우가 자동 실행된다. 수동 실행도 가능(Actions 탭 → Deploy to EC2 → Run workflow).

### F-3. 상태 확인
```bash
# EC2에서
cd ~/bodeum
docker compose ps
docker compose logs -f app        # 부팅 로그, DB 연결/validate 확인
```
```bash
# 외부에서
curl -I https://<도메인>/swagger-ui/index.html   # 200 이면 정상
```

### F-4. 스모크 테스트
- Swagger: `https://<도메인>/swagger-ui/index.html`
- 소셜 로그인 리다이렉트: `https://<도메인>/api/v1/auth/login/kakao`

---

## 트러블슈팅

| 증상 | 원인/조치 |
|---|---|
| Actions에서 `docker login`/pull 실패 | GHCR 권한. 워크플로우 `packages: write/read` 및 `GITHUB_TOKEN` 확인 |
| SSH 단계 timeout | `EC2_HOST`/보안그룹 22번/`EC2_SSH_KEY` 개행 포함 여부 확인 |
| 앱이 DB 연결 실패 | RDS 보안그룹(3306, EC2 소스), `DB_URL` 엔드포인트/스키마명 확인 |
| `Schema validation: missing table/column` | Part A-3 스키마 부트스트랩 재수행 |
| 502 Bad Gateway | 앱 컨테이너 미기동 or 8080 미바인딩. `docker compose logs app` |
| OAuth `redirect_uri_mismatch` | 콘솔 등록값과 `.env` 리다이렉트 URI 불일치 |
| 크롤링(Selenium) 시 Chrome 오류 | 런타임 이미지에 Chrome 포함됨. 메모리 부족이면 인스턴스 사양 상향 |

## 롤백
특정 커밋 이미지로 되돌리기:
```bash
cd ~/bodeum
docker pull ghcr.io/umc-10th-bodeum/bodeum-backend:<이전_SHA>
docker tag ghcr.io/umc-10th-bodeum/bodeum-backend:<이전_SHA> ghcr.io/umc-10th-bodeum/bodeum-backend:latest
docker compose up -d
```
