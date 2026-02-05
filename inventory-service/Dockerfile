# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy parent pom and module poms
COPY pom.xml ./
COPY oms-common/pom.xml oms-common/
COPY inventory-service/pom.xml inventory-service/
COPY order-service/pom.xml order-service/

# Copy source code for relevant modules
COPY oms-common/src oms-common/src
COPY inventory-service/src inventory-service/src

# Install Maven and build
RUN apk add --no-cache maven
RUN mvn clean package -pl oms-common,inventory-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy jar from build stage
COPY --from=build /app/inventory-service/target/*.jar app.jar

# Expose port
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
