FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/auth-server.jar app.jar
EXPOSE 9000
ENTRYPOINT ["java", "-jar", "app.jar"]

