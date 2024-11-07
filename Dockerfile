# Stage 1: Build the JAR file
FROM maven:3.8.5-openjdk-17-slim AS builder
 
# Install Maven dependencies
COPY pom.xml ./
RUN mvn dependency:go-offline
 
# Copy the source code
COPY . .
 
# Build the JAR file
RUN mvn package
 
# Stage 2: Create the final image
FROM openjdk:17
 
# Copy the JAR file from the builder stage
COPY --from=builder /target/springioapi-0.0.1-SNAPSHOT.jar /app/springioapi-0.0.1-SNAPSHOT.jar
 
# Set the working directory
WORKDIR /app
 
# Expose the application port
EXPOSE 8080
 
# Entrypoint to run the application
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app/springioapi-0.0.1-SNAPSHOT.jar"]