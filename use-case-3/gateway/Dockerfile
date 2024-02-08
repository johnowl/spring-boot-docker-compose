FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app
ADD ../ /app/
RUN mvn clean verify -pl gateway

FROM openjdk:17-jdk-bullseye
COPY --from=build /app/gateway/target/*.jar /app.jar
EXPOSE 9090
ENTRYPOINT java -jar /app.jar