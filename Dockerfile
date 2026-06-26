FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN groupadd -r spring && useradd -r -g spring spring \
    && mkdir -p /app/uploads \
    && chown -R spring:spring /app

COPY --from=build --chown=spring:spring /workspace/target/MyBill_Backend-1.0.0.jar app.jar

USER spring:spring
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
