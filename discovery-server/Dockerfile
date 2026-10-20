FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/discovery-server-*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-XX:+UseZGC", "-jar", "/app/app.jar"]