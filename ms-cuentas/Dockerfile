FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/ms-cuentas.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]