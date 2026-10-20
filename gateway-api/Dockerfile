FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/gateway-api-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-XX:+UseZGC", "-jar", "/app/app.jar"]