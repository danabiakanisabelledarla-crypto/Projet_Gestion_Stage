# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:26-jdk AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q -DskipTests dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -q -DskipTests clean package \
    && cp "$(find target -maxdepth 1 -type f -name '*.jar' ! -name '*.original' | head -n 1)" /workspace/application.jar

FROM eclipse-temurin:26-jre

WORKDIR /app

RUN groupadd --system spring \
    && useradd --system --gid spring --home-dir /app spring \
    && mkdir -p /app/data /app/uploads \
    && chown -R spring:spring /app

COPY --from=build --chown=spring:spring /workspace/application.jar /app/application.jar

USER spring:spring

EXPOSE 8080

ENV JAVA_OPTS="" \
    SERVER_PORT=8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/application.jar"]
