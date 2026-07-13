FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package \
    && mkdir -p /runtime/uploads

FROM gcr.io/distroless/java17-debian12:nonroot
WORKDIR /app

ENV TZ=Asia/Kolkata
ENV APP_TIME_ZONE=Asia/Kolkata
ENV JAVA_TOOL_OPTIONS="-Xms64m -Xmx256m -XX:+UseContainerSupport -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=48m -XX:MaxDirectMemorySize=32m -Xss512k -XX:ActiveProcessorCount=1 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp -Xlog:gc*,safepoint:file=/tmp/mybill-gc.log:time,uptime,level,tags:filecount=3,filesize=5M -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/urandom"

COPY --from=build --chown=nonroot:nonroot /workspace/target/MyBill_Backend-1.0.0.jar /app/app.jar
COPY --from=build --chown=nonroot:nonroot /runtime/uploads /app/uploads

USER nonroot:nonroot
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
