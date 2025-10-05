# Stage 1: build with Maven
FROM maven:3.8.5-openjdk-17 AS builder
WORKDIR /app

# copy only what is needed first to leverage docker cache
COPY pom.xml .
# copy mvnw/.mvn if present to help reproducible builds
COPY .mvn .mvn
COPY mvnw mvnw

# copy source and build
COPY src ./src
RUN mvn -B -DskipTests clean package

# Stage 2: runtime image
FROM openjdk:17-jdk-slim
# create non-root user
RUN groupadd -r app && useradd -r -g app app

WORKDIR /app
# Copy built jar (matches target/*.jar)
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

# run as non-root user
USER app

# start the app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]