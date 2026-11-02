# stage 1: build front end at the same time so no manual :build command is needed
FROM node:22-alpine AS frontend
WORKDIR /frontend
COPY frontend/package*.json ./
RUN npm ci                      
COPY frontend/ ./
ENV VITE_API_URL=/api           
RUN npm run build               

# stage 2: build backend jar + frontend build copied into static/ (DRIFT.md "frontend i V2" said to do this)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY backend/ResursPortal/pom.xml .
RUN mvn dependency:resolve -q
COPY backend/ResursPortal/src ./src
COPY --from=frontend /frontend/dist ./src/main/resources/static
RUN mvn package -DskipTests -q

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN mkdir -p /tmp/uploads
COPY --from=build /app/target/resurs-portal-1.0-SNAPSHOT.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
