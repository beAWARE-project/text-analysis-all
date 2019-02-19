FROM openjdk:8

COPY babelnet_config /resources/babelnet_config

COPY target/text-analysis-all-standalone.jar .

WORKDIR .

CMD ["java", "-Xmx4G", "-jar", "text-analysis-all-standalone.jar"]
