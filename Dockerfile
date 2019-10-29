FROM openjdk:9-jre

COPY babelnet_config /babelnet_config

COPY target/text-analysis-all-standalone.jar .

WORKDIR .

CMD ["java", "-Xmx8G", "-jar", "text-analysis-all-standalone.jar"]
