# 1단계: 빌드 환경 설정
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Gradle 래퍼와 의존성 파일만 먼저 복사하여 라이브러리 캐싱 활용
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사 및 빌드 (라이브러리 다운로드 단계 건너뜀)
COPY src src
RUN ./gradlew clean bootJar -x test --no-daemon

# 2단계: 실행 환경 (JRE만 사용하여 이미지 크기 축소)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 보안을 위해 root가 아닌 일반 사용자 계정 사용
RUN addgroup -S spring && adduser -S spring -G spring
RUN mkdir -p /app/logs && chown -R spring:spring /app
COPY --from=build /app/build/libs/*.jar app.jar

USER spring:spring

# JVM 메모리 최적화 옵션 유지
ENTRYPOINT ["java","-Xms256m","-Xmx512m","-jar","app.jar"]