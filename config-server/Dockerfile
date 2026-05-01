# Use official lightweight Java 21 image
FROM eclipse-temurin:21-jre-alpine

# Set non-root user for security (optional but best practice)
RUN addgroup -S spring && adduser -S spring -G spring
RUN mkdir /logs && chown spring:spring /logs
USER spring:spring

# Optimize JVM settings inside container
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Copy the pre-built JAR from the target directory
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
COPY config-repo /config-repo

# Run the application
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app.jar"]
