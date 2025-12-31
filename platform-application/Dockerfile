# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml ./
COPY platform-application/pom.xml platform-application/pom.xml

# Copy the rest of the sources (multi-module)
COPY . ./

RUN mvn -pl platform-application -am -DskipTests package

# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Container-aware JVM defaults: use up to ~80% of cgroup memory for heap
# Can be overridden via JAVA_TOOL_OPTIONS in docker-compose if needed.
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:InitialRAMPercentage=40.0 -XX:MaxRAMPercentage=80.0"

COPY --from=build /workspace/platform-application/target/platform-application-*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
