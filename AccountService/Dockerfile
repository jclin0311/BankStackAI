# Runtime-only image: the jar is built by Maven outside Docker
# (locally via build-all.sh, in CI by the workflow) and copied in.
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Run as non-root
RUN useradd --system --uid 1001 appuser
USER appuser

COPY target/*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
