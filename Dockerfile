FROM openjdk:11
EXPOSE 8080
ADD target/zeebe-send-email-worker.jar zeebe-send-email-worker.jar
ENTRYPOINT ["java","-jar","/zeebe-send-email-worker.jar"]

