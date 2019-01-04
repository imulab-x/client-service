FROM openjdk:8-jdk-alpine

COPY ./build/libs/client-service-*.jar client-service.jar

ENTRYPOINT ["java", "-jar", "/client-service.jar"]