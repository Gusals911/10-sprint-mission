# build stage
FROM gradle:8.10-jdk17 AS build
WORKDIR /app

# 캐시 최적화: 의존성 관련 파일 먼저 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

# 소스는 마지막에 복사
COPY src src

# jar 빌드
RUN ./gradlew bootJar --no-daemon

# runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 80
ENTRYPOINT ["java", "-jar", "app.jar"]