FROM openjdk:9-jre


RUN mkdir /app
#RUN mkdir /resources

COPY babelconfig-4.0.1 /app/babelnet_config
COPY disambiguation_en.properties /app/
COPY disambiguation_es.properties /app/
#COPY fire_meanings.txt /resources/
#COPY synsets_3rd_pilot.bin /resources/
#COPY /datasets/TALN/embeddings/word-vectors/glove/glove.840B.300d.ra /resources/
#COPY /datasets/TALN/embeddings/word-vectors/fasttext/cc.es.300.ra /resources/
#COPY /datasets/TALN/BabelNet-4.0.1_es.cache /resources/
#COPY /datasets/TALN/BabelNet-4.0.1_en.cache /resources/
#COPY /datasets/TALN/WordNet-3.0/ /resources/WordNet-3.0

COPY target/text-analysis-all-standalone.jar /app

WORKDIR /app

CMD ["java", "-Xmx8G", "-jar", "text-analysis-all-standalone.jar"]
