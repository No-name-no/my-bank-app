FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/cash-service-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-XX:+UseZGC", "-jar", "/app/app.jar"]