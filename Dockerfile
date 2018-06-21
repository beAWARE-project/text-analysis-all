FROM openjdk:8

COPY target/text-analysis-all-standalone.jar .

WORKDIR .

CMD ["java", "-jar", "text-analysis-all-standalone.jar"]
