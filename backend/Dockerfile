# Build stage
FROM maven:3.9.5-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy only what is needed to build to leverage Docker cache
COPY pom.xml ./
COPY .mvn/ .mvn/
COPY mvnw ./mvnw
COPY src ./src

# Ensure mvnw is executable (for environments where it isn't)
RUN if [ -f ./mvnw ] ; then chmod +x ./mvnw ; fi

# Build the project (skip tests by default to speed up builds)
RUN mvn -B -DskipTests package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the fat jar produced by the build stage
# The target directory may contain a single executable jar — use wildcard
COPY --from=build /workspace/target/*-SNAPSHOT.jar app.jar

# Fallback: if the above didn't match, also attempt any jar
COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080

# Use a non-root user for better security (optional)
RUN addgroup --system appgroup && adduser --system appuser --ingroup appgroup || true
USER appuser

ENTRYPOINT ["java","-jar","/app/app.jar"]
