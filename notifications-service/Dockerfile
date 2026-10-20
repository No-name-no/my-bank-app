FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/notifications-service-*.jar app.jar
EXPOSE 8089
ENTRYPOINT ["java", "-XX:+UseZGC", "-jar", "/app/app.jar"]