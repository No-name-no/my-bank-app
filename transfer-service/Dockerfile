FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/transfer-service-*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-XX:+UseZGC", "-jar", "/app/app.jar"]