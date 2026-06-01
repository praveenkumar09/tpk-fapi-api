# ── Build stage ──────────────────────────────────────────────────────────────
# Maven 3.9 + Eclipse Temurin JDK 21 on Alpine Linux as the build environment.
# Alpine keeps the image small; Temurin is the OpenJDK distribution from Adoptium.
FROM maven:3.9-eclipse-temurin-21-alpine AS build

# Set the working directory inside the build container.
# All subsequent COPY/RUN commands operate relative to this path.
WORKDIR /build

# Copy only pom.xml first so Docker can cache the dependency-download layer.
# If source files change but pom.xml doesn't, this layer is reused from cache.
COPY pom.xml .

# Download all Maven dependencies into the local repository inside the container.
# -q suppresses verbose output. This layer is cached until pom.xml changes.
RUN mvn dependency:go-offline -q

# Copy the full source tree now (after deps are cached).
COPY src/ src/

# Compile and package the application using the Quarkus Maven plugin.
# -DskipTests skips unit/integration tests — tests run in CI, not during image build.
# The Quarkus fast-jar output lands at target/quarkus-app/.
RUN mvn package -DskipTests -q

# ── Runtime stage ─────────────────────────────────────────────────────────────
# Eclipse Temurin JRE 21 on Alpine — only the JRE (not full JDK) to keep the
# final image as small as possible. No compiler or build tools needed at runtime.
FROM eclipse-temurin:21-jre-alpine

# Working directory for the running application.
# quarkus-run.jar references app/, lib/, and quarkus/ as relative paths,
# so all four directories must be co-located under the same WORKDIR.
WORKDIR /app

# Copy the entire Quarkus fast-jar layout from the build stage.
# fast-jar structure:
#   quarkus-run.jar  — thin launcher
#   app/             — application classes
#   lib/             — all dependency jars
#   quarkus/         — Quarkus augmentation metadata
COPY --from=build /build/target/quarkus-app/ .

# Declare the port the application listens on (matches quarkus.http.port=8080).
# This is metadata only — it does not publish the port; that is done at runtime.
EXPOSE 8080

# Start the application. java -jar quarkus-run.jar is the standard Quarkus
# fast-jar entry point. Override env vars at docker run / compose / K8s time.
ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]