FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

COPY . .
RUN ./gradlew clean build

FROM eclipse-temurin:17-jdk
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

ENTRYPOINT ["java","-Xms256m","-Xmx512m","-jar","app.jar"]