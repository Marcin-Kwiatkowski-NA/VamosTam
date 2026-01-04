# Stage 1: Build
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY . .
# Skip tests to speed up build, remove -DskipTests if you want them
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:25-jdk
WORKDIR /app
# Copy the built jar from the build stage
COPY --from=build /blablatwoApp/target/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
