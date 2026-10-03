FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/ms-transacciones.jar app.jar
EXPOSE 8092
ENTRYPOINT ["java", "-jar", "app.jar"]

