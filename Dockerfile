FROM eclipse-temurin:21-jre
WORKDIR /app
COPY ./target/chat.jar /app/chat.jar
ENTRYPOINT ["java", "-jar", "chat.jar"]
CMD []