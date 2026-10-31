FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/ledgerflow-banking-api-*.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
