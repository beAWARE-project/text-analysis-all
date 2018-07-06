package edu.upf.taln.beaware.uima.textAnalysis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCopier;

import com.google.gson.GsonBuilder;
import com.jayway.jsonpath.JsonPath;

import de.tudarmstadt.ukp.dkpro.core.arktools.ArktweetTokenizer;
import de.tudarmstadt.ukp.dkpro.core.castransformation.ApplyChangesAnnotator;
import edu.upf.taln.beaware.uima.consumer.BeAwareKafkaConsumer;
import edu.upf.taln.beaware.uima.pipeline.BeawarePipeline;
import edu.upf.taln.beaware.uima.pipeline.EnglishPipelineUd;
import edu.upf.taln.beaware.uima.pipeline.GreekPipeline;
import edu.upf.taln.beaware.uima.pipeline.ItalianPipeline;
import edu.upf.taln.beaware.uima.pipeline.SpanishPipelineUd;
import edu.upf.taln.beaware.uima.types.BeAwareMetaData;
import edu.upf.taln.uima.clean.twitter_clean.CleanTokens;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

public class TextAnalysisRouter extends JCasAnnotator_ImplBase{

	Logger logger = Logger.getLogger(TextAnalysisRouter.class.toString());

	public static final String TARGET_VIEW = "TargetView";

	/**
	 * List of Kafka brokers
	 */
	public static final String PARAM_KAFKABROKERS = "KafkaBrokers";
	@ConfigurationParameter(name=PARAM_KAFKABROKERS, mandatory=false, defaultValue="",
			description="List of Kafka brokers")
	private String kafkaBrokers;

	/**
	 * List of Kafka brokers
	 */
	public static final String PARAM_KAFKAKEY = "KafkaApiKey";
	@ConfigurationParameter(name=PARAM_KAFKAKEY, mandatory=false, defaultValue="",
			description="Kafka API key")
	private String kafkaApiKey;

	private Map<String, AnalysisEngine> pipes;

	private AnalysisEngine cleaner;

	private AnalysisEngine kafkaWriter;

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);

		this.kafkaWriter = createEngine(createEngineDescription(BeAwareKafkaConsumer.class,
				BeAwareKafkaConsumer.PARAM_KAFKATOPIC,"TOP028_TEXT_ANALYSED",
				BeAwareKafkaConsumer.PARAM_KAFKABROKERS, kafkaBrokers,
				BeAwareKafkaConsumer.PARAM_KAFKAKEY, kafkaApiKey
				));

		try {
			Map<String,BeawarePipeline>builders = new HashMap<>();
			builders.put("en", new EnglishPipelineUd());
			builders.put("es", new SpanishPipelineUd());
			builders.put("el", new GreekPipeline());
			builders.put("it", new ItalianPipeline());

			this.pipes = new HashMap<>();
			Map<String, String> options = new HashMap<String, String>();
			for (String lang : builders.keySet()) {
				this.pipes.put(lang, createEngine(builders.get(lang).build(options)));
			}

			AggregateBuilder builder = new AggregateBuilder();

			// tweet cleaning
			builder.add(AnalysisEngineFactory.createEngineDescription(ArktweetTokenizer.class));
			builder.add(AnalysisEngineFactory.createEngineDescription(CleanTokens.class));
			//apply changes from default sofa to target view
			builder.add(AnalysisEngineFactory.createEngineDescription(
					ApplyChangesAnnotator.class),
					ApplyChangesAnnotator.VIEW_TARGET, TARGET_VIEW,
					ApplyChangesAnnotator.VIEW_SOURCE, CAS.NAME_DEFAULT_SOFA);
			this.cleaner = builder.createAggregate();
		} catch (UIMAException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void process(JCas kafkaCas) throws AnalysisEngineProcessException {

		String kafkaMessage = kafkaCas.getDocumentText();
		String topic = "unknown"; 
		try {
			logger.info("received message: " + kafkaMessage);

			String filterBody = "$[?(@.body)]";
			List<String> matchesBody = JsonPath.read(kafkaMessage, filterBody);
			if (matchesBody.isEmpty()) {
				logger.info("no body found, rejected: " + kafkaMessage);
				return;
			}

			try {
				topic = ((List<String>)JsonPath.read(kafkaMessage, "$..topicName")).get(0);
				logger.info("topic: "+topic);
			} catch (Exception e) {
				logger.warning("couldn't get topic");
				logger.info(kafkaMessage);
			}
			// filters
			// don't process actionType=update from APP
			if (topic.equals("TOP021_INCIDENT_REPORT")) {
				String filterApp = "$[?(@.body.description && @.header.actionType == 'Alert')]";
				List<String> matches = JsonPath.read(kafkaMessage, filterApp);
				if (matches.isEmpty()) {
					logger.info("rejected by filter: " + kafkaMessage);
					return;
				}
			}

			// treat tweets differently
			boolean isTwitter = false;
			/*
			if ("TOP001_SOCIAL_MEDIA_TEXT".equals(topic)) {
				isTwitter = true;
			};
			*/

			// build CAS for processing (like BeAwareKafkaIncidentReader)
			JCas jcas = messageToCas(kafkaMessage);

			// process with appropriate pipeline
			String lang = jcas.getDocumentLanguage();
			if (!pipes.containsKey(lang)) {
				logger.info("unknown language: "+lang);
				return; // skip unknown languages
			}
			if (isTwitter) {
				try {
					this.cleaner.process(jcas);
					JCas cleanView = jcas.getView(TARGET_VIEW);
					this.pipes.get(lang).process(cleanView);
					BeAwareMetaData meta = JCasUtil.selectSingle(jcas, BeAwareMetaData.class);
					BeAwareMetaData meta2 = (BeAwareMetaData) meta.clone();
					meta2.setFeatureValue(meta2.getType().getFeatureByBaseName("sofa"), cleanView.getSofa());
					meta2.addToIndexes(cleanView);
					this.kafkaWriter.process(cleanView);
				} catch (CASException|AnalysisEngineProcessException e) {
					logger.warning(e.toString());
					throw new AnalysisEngineProcessException(e);
				}
			} else { // not Twitter
				this.pipes.get(lang).process(jcas);
				this.kafkaWriter.process(jcas);
			}
		} catch (Exception e) {
			logger.severe("skipping message:" + kafkaMessage);
			logger.severe(e.getStackTrace().toString());
		}
	}

	private JCas messageToCas(String kafkaMessage) throws AnalysisEngineProcessException {
		try {
			JCas jcas = JCasFactory.createJCas();
			GsonBuilder builder = new GsonBuilder();
			Map<String, Object> o = (Map<String, Object>) builder.create().fromJson(kafkaMessage, Object.class);
			Map<String, Object> body = (Map<String, Object>) o.get("body");
			try {
				jcas.setDocumentText((String) body.get("description"));
			} catch (NullPointerException e) {
				jcas.setDocumentText("");
			}
			if (jcas.getDocumentText() == null) { //probably can't happen
				jcas.setDocumentText("");
			}
			BeAwareMetaData metadata = new BeAwareMetaData(jcas);
			metadata.setKafkaMessage(kafkaMessage);
			metadata.setDocumentId(body.get("incidentID").toString());
			jcas.setDocumentLanguage(((String) body.get("language")).replaceAll("-.*", ""));
			if (metadata.getView().getDocumentText() != null) {
				metadata.setBegin(0);
				metadata.setEnd(metadata.getView().getDocumentText().length());
			}
			metadata.addToIndexes();
			return jcas;
		} catch (UIMAException e1) {
			throw new AnalysisEngineProcessException(e1);
		}

	}

}
