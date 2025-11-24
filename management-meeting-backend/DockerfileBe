# ============================
# STAGE 1: Build the application
# ============================
FROM maven:3.9.9-eclipse-temurin-21 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies (for caching)
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copy entire project source
COPY . .

# Build the Spring Boot application (skip tests for faster build)
RUN mvn clean package -DskipTests

# ============================
# STAGE 2: Run the application
# ============================
FROM eclipse-temurin:21-jre-jammy

# Set working directory inside the container
WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/fashion-store-0.0.1-SNAPSHOT.jar ./app.jar
COPY .env .
# Expose the default Spring Boot port
EXPOSE 8080

# Environment variable for optional JVM options
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Command to run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
