# ------------------------------------------------------------------------------
# STAGE 1: Build & Dependency Resolution
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /workspace

# 1. Cache Maven dependencies separately from source code
COPY pom.xml .
RUN apt-get update && apt-get install -y maven && mvn dependency:go-offline -B

# 2. Copy source code and package application JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# ------------------------------------------------------------------------------
# STAGE 2: Minimal, Hardened Production Runtime
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runner
WORKDIR /app

# Security: Create non-root system group and user
RUN groupadd -r appgroup && useradd -r -g appgroup -s /sbin/nologin -d /app appuser

# Copy executable JAR from builder stage
COPY --from=builder --chown=appuser:appgroup /workspace/target/*.jar app.jar

# Switch to unprivileged user
USER appuser:appgroup

# Expose HTTP port and Actuator metrics port
EXPOSE 8080 8081

# Container-aware JVM tuning for Java 21:
# - MaxRAMPercentage=75.0 allows JVM to automatically scale heap to container limits
# - ZGenerational GC eliminates latency spikes in high-throughput AI workloads
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseZGC -XX:+ZGenerational -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
