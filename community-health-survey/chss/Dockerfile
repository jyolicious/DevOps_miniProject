# Build the existing Spring Boot executable WAR.
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml ./pom.xml
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress -DskipTests package \
    && test -s target/community-health-survey.war

# Run on a Java 17 JRE as an unprivileged user.
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
ENV APP_PORT=8081
EXPOSE 8081

RUN groupadd --system chss \
    && useradd --system --gid chss --home-dir /app --shell /usr/sbin/nologin chss
COPY --from=build --chown=chss:chss /workspace/target/community-health-survey.war /app/app.war

USER chss
ENTRYPOINT ["java", "-jar", "/app/app.war"]
