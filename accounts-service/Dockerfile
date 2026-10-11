FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/accounts-service-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-XX:+UseZGC", "-jar", "/app/app.jar"]