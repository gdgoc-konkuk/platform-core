# Use a multi-architecture base image that supports ARM64
FROM eclipse-temurin:17-jdk

# Set the working directory in the container
WORKDIR /app

# Copy the executable jar file to the working directory
COPY build/libs/platform-core-*.jar app.jar

# Expose the port the application runs on
EXPOSE 8080

# Command to run the executable jar file
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
