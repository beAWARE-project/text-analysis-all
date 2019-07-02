package edu.upf.taln.beaware.uima.textAnalysis;


import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import java.util.Optional;
import java.util.logging.Logger;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.JCasIterable;
import org.apache.uima.jcas.JCas;

import edu.upf.taln.beaware.kafka.KafkaReader;

/**
 * This pipeline does text analysis for all languages and sources
 */
public class TextAnalysisPipeline {

	public static void main(String[] args) throws Exception {

		Logger logger = Logger.getLogger(TextAnalysisPipeline.class.toString());

		String kafkaBrokers = System.getenv("SECRET_MH_BROKERS");
		String kafkaApiKey = System.getenv("SECRET_MH_API_KEY");
		String groupId = Optional.ofNullable(System.getenv("KAFKA_GROUPID")).orElse("text-analysis-all");

		// setup components
		CollectionReaderDescription reader = createReaderDescription(KafkaReader.class,
				KafkaReader.PARAM_KAFKATOPIC,"TOP010_AUDIO_ANALYZED,TOP001_SOCIAL_MEDIA_TEXT,TOP021_INCIDENT_REPORT",
				KafkaReader.PARAM_KAFKABROKERS, kafkaBrokers,
				KafkaReader.PARAM_KAFKASEEKTOEND, true,
				KafkaReader.PARAM_KAFKAKEY, kafkaApiKey,
				KafkaReader.PARAM_GROUPID, groupId
				);
		AnalysisEngineDescription ae = createEngineDescription(TextAnalysisRouter.class,
				TextAnalysisRouter.PARAM_KAFKABROKERS, kafkaBrokers,
				TextAnalysisRouter.PARAM_KAFKAKEY, kafkaApiKey
				);

		// configure pipeline
		JCasIterable pipeline = new JCasIterable(reader, ae);

		// Run and show results in console
		logger.info("starting pipeline");
		for (JCas jcas : pipeline) {
		}
	}


}
