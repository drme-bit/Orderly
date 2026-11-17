# ---------- build ----------
FROM eclipse-temurin:27-jdk AS builder
WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# ---------- runtime ----------
FROM eclipse-temurin:27-jre AS runner
WORKDIR /app

RUN useradd --system --uid 1001 appuser

COPY --from=builder /app/build/libs/*.jar app.jar

USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
