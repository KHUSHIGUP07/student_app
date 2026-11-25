FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn --batch-mode verify

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/student-management-1.0.0.jar app.jar
USER 10001
EXPOSE 8095
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
