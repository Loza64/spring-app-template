FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline

COPY src src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jdk
WORKDIR /app


COPY --from=build /app/target/*.jar app.jar

EXPOSE 4000
ENTRYPOINT ["java", "-jar", "app.jar"]

# docker run -p 4000:4000 nombre-de-tu-imagen