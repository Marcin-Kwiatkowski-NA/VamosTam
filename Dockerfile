# Stage 1: Build
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY . .
# Skip tests to speed up build, remove -DskipTests if you want them
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:25-jdk
WORKDIR /app

# Install both curl and wget for Coolify's health checks and debugging
RUN apt-get update && apt-get install -y curl wget && rm -rf /var/lib/apt/lists/*

# Copy the built jar from the build stage
COPY --from=build /app/blablatwoApp/target/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
