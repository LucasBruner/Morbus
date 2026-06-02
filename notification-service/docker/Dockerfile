FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B -q

COPY src/ src/
RUN ./mvnw package -DskipTests -B -q \
    -Dquarkus.package.jar.type=fast-jar

FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /app/target/quarkus-app/lib/     ./lib/
COPY --from=builder /app/target/quarkus-app/quarkus/ ./quarkus/
COPY --from=builder /app/target/quarkus-app/app/     ./app/
COPY --from=builder /app/target/quarkus-app/*.jar    ./

USER appuser

EXPOSE 8081

ENV QUARKUS_PROFILE=prod
ENV QUARKUS_DATASOURCE_JDBC_URL=""
ENV QUARKUS_DATASOURCE_USERNAME=""
ENV QUARKUS_DATASOURCE_PASSWORD=""
ENV RABBITMQ_HOST=""
ENV RABBITMQ_PORT="5672"
ENV RABBITMQ_USERNAME=""
ENV RABBITMQ_PASSWORD=""

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.util.logging.manager=org.jboss.logmanager.LogManager", \
  "-jar", "quarkus-run.jar"]
