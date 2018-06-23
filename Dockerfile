FROM openjdk:8

COPY target/text-analysis-all-standalone.jar .

WORKDIR .

CMD ["java", "-Xmx4G", "-jar", "text-analysis-all-standalone.jar"]
