FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/front-app-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseZGC", "-jar", "/app/app.jar"]