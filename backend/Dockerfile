# Simple Dockerfile — expects backend JAR to be built in target/
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# copy built jar (use wildcard to avoid hardcoding name)
COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]
