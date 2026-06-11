FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw package -B

FROM eclipse-temurin:25-jre
WORKDIR /app

RUN addgroup --system marketplace && adduser --system --ingroup marketplace marketplace

COPY --from=build /app/target/*.jar app.jar

RUN chown marketplace:marketplace app.jar

USER marketplace

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
