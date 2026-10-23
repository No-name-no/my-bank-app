FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/config-server-*.jar app.jar
EXPOSE 8760
ENTRYPOINT ["java", "-XX:+UseZGC", "-jar", "/app/app.jar"]