# --- Build stage ---
# pom.xml targets Java 25; eclipse-temurin:25 has been available since Sep 2025
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Cache dependency downloads
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

COPY src ./src
RUN mvn package -DskipTests -B -q

# --- Runtime stage ---
FROM eclipse-temurin:25-jre-noble
WORKDIR /app

RUN groupadd -r frictionlens && useradd -r -g frictionlens frictionlens
COPY --from=build /app/target/*.jar app.jar
RUN chown frictionlens:frictionlens app.jar

USER frictionlens
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
