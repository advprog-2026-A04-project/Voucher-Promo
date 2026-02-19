# syntax=docker/dockerfile:1

FROM node:20-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM eclipse-temurin:17-jdk AS backend-build
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew gradlew
COPY gradlew.bat gradlew.bat
COPY settings.gradle.kts build.gradle.kts ./
COPY backend/ backend/

# Serve the Vite build from Spring Boot's static resources.
COPY --from=frontend-build /app/frontend/dist backend/src/main/resources/static

RUN ./gradlew :backend:bootJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=backend-build /app/backend/build/libs/*.jar /app/app.jar
EXPOSE 8080
ENV JAVA_OPTS=""
CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

