# Multi-stage: build with Maven+JDK, run on a slim JRE. Built natively on Mack (ARM64) -
# same reasoning as KrakenBot/SolanaSniper: cross-compiling on the Windows dev machine hits
# QEMU issues for anything non-trivial, so the tarball-and-build-on-Mack pattern (deploy-mack.sh)
# is used instead of `docker buildx --platform linux/arm64` from a different host.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
# Cache dependency resolution in its own layer - only re-runs when pom.xml changes.
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/dealtracker.jar app.jar
# /data holds the SQLite file - mounted as a volume so it survives container recreation.
VOLUME /data
ENV DEALTRACKER_DB_PATH=/data/dealtracker.db
ENTRYPOINT ["java", "-jar", "app.jar"]
