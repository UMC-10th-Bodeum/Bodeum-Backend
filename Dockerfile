# syntax=docker/dockerfile:1

# ---------- 1단계: 빌드 ----------
# JDK 21로 gradle 빌드만 수행 (테스트는 배포 속도를 위해 제외)
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

# 의존성 캐시를 위해 gradle 관련 파일 먼저 복사
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x ./gradlew && ./gradlew dependencies --no-daemon || true

# 소스 복사 후 부트 jar 빌드
COPY src ./src
RUN ./gradlew clean bootJar -x test --no-daemon \
 && cp build/libs/*-SNAPSHOT.jar app.jar

# ---------- 2단계: 실행 ----------
# JRE 21 + Selenium 크롤링용 Chrome 설치
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# Google Chrome 안정판 설치 (Selenium 동적 크롤링에 필요)
# 크롤링을 호출하지 않으면 앱 부팅에는 영향 없음
RUN apt-get update \
 && apt-get install -y --no-install-recommends wget gnupg ca-certificates fonts-liberation \
 && wget -q -O /tmp/chrome.deb https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb \
 && apt-get install -y --no-install-recommends /tmp/chrome.deb \
 && rm -f /tmp/chrome.deb \
 && apt-get clean \
 && rm -rf /var/lib/apt/lists/*

# 루트가 아닌 사용자로 실행
RUN useradd -m -u 1000 appuser
USER appuser

COPY --from=build /workspace/app.jar app.jar

# dev 프로파일(운영: ddl-auto=validate)로 구동
ENV SPRING_PROFILES_ACTIVE=dev
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

EXPOSE 8080
# exec로 실행해야 쉘이 java로 대체되어 컨테이너 종료 시 SIGTERM이 앱에 전달된다(graceful shutdown)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
