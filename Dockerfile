FROM openjdk:8

COPY babelnet_config /babelnet_config

COPY target/text-analysis-all-standalone.jar .

WORKDIR .

CMD ["java", "-Xmx8G", "-jar", "text-analysis-all-standalone.jar"]
