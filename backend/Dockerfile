# Multi-stage build for JiVS Platform Backend

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S jivs && adduser -S jivs -G jivs

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Create directories for logs and storage
RUN mkdir -p /app/logs /var/jivs/storage && \
    chown -R jivs:jivs /app /var/jivs

# Switch to non-root user
USER jivs

# Expose port (Railway will override with $PORT)
EXPOSE 8080

# Run application with Railway's PORT variable
CMD java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=production -Dserver.port=${PORT:-8080} -jar app.jar
