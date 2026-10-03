FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/bff-mobile.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]

