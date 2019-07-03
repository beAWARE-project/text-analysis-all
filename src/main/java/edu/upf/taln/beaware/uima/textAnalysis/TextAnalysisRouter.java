package edu.upf.taln.beaware.uima.textAnalysis;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.internal.MetaDataUtil;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import com.google.common.base.Throwables;
import com.google.gson.GsonBuilder;
import com.jayway.jsonpath.JsonPath;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.arktools.ArktweetTokenizer;
import de.tudarmstadt.ukp.dkpro.core.castransformation.ApplyChangesAnnotator;
import edu.upf.taln.beaware.analysis.EnglishPipelineUD;
import edu.upf.taln.beaware.analysis.GreekPipelineUD;
import edu.upf.taln.beaware.analysis.ItalianPipelineUD;
import edu.upf.taln.beaware.analysis.SpanishPipelineUD;
import edu.upf.taln.beaware.analysis.pojos.AnalysisConfigurationEL;
import edu.upf.taln.beaware.analysis.pojos.AnalysisConfigurationEN;
import edu.upf.taln.beaware.analysis.pojos.AnalysisConfigurationES;
import edu.upf.taln.beaware.analysis.pojos.AnalysisConfigurationIT;
import edu.upf.taln.beaware.kafka.AnalysisKafkaConsumer;
import edu.upf.taln.beaware.kafka.types.BeAwareMetaData;
import edu.upf.taln.uima.clean.twitter_clean.CleanTokens;

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

	public void initializePipelines() throws ResourceInitializationException {

		try {
			/*Map<String,IBeawarePipeline>builders = new HashMap<>();
			builders.put("en", new EnglishPipelineUD());
			builders.put("es", new SpanishPipelineUD());
			builders.put("el", new GreekPipelineUD());
			builders.put("it", new ItalianPipelineUD());*/

			Optional<String> babelnetConfigPath = Optional.ofNullable(System.getenv("BABELNET_CONFIG"));
			Optional<String> conceptEnUrl = Optional.ofNullable(System.getenv("CONCEPT_URL"));
			Optional<String> conceptEsUrl = Optional.ofNullable(System.getenv("CONCEPT_ES_URL"));
			Optional<String> geolocationUrl = Optional.ofNullable(System.getenv("GEOLOCATION_URL"));
			Optional<String> nerEnUrl = Optional.ofNullable(System.getenv("NER_EN_URL"));
			Optional<String> nerEsUrl = Optional.ofNullable(System.getenv("NER_ES_URL"));
			Optional<String> nerElUrl = Optional.ofNullable(System.getenv("NER_ES_URL")); //TODO: add EL NER
			Optional<String> nerItUrl = Optional.ofNullable(System.getenv("NER_ES_URL")); //TODO: add IT NER

			this.pipes = new HashMap<>();
			
			AnalysisConfigurationEN enConf = new AnalysisConfigurationEN();
			enConf.setBabelnetConfigPath(babelnetConfigPath.get());
			enConf.setGeolocationUrl(geolocationUrl.get());
			enConf.setNerUrl(nerEnUrl.get());
			enConf.setCandidateConceptsUrl(conceptEnUrl.get());
			
			AnalysisConfigurationES esConf = new AnalysisConfigurationES();
			esConf.setBabelnetConfigPath(babelnetConfigPath.get());
			esConf.setGeolocationUrl(geolocationUrl.get());
			esConf.setNerUrl(nerEsUrl.get());
			esConf.setCandidateConceptsUrl(conceptEsUrl.get());
			
			AnalysisConfigurationEL elConf = new AnalysisConfigurationEL();
			elConf.setBabelnetConfigPath(babelnetConfigPath.get());
			elConf.setGeolocationUrl(geolocationUrl.get());
			elConf.setNerUrl(nerElUrl.get());
			
			AnalysisConfigurationIT itConf = new AnalysisConfigurationIT();
			itConf.setBabelnetConfigPath(babelnetConfigPath.get());
			itConf.setGeolocationUrl(geolocationUrl.get());
			itConf.setNerUrl(nerItUrl.get());
			
			/*Map<String, String> options = new HashMap<String, String>();
			options.put("babelnet", "/babelnet_config");
			options.put("similFile", "/resources/sensembed-vectors-merged_bin");*/
			
			this.pipes.put("en", createEngine(EnglishPipelineUD.getPipelineDescription(enConf)));
			this.pipes.put("es", createEngine(SpanishPipelineUD.getPipelineDescription(esConf)));
			this.pipes.put("it", createEngine(ItalianPipelineUD.getPipelineDescription(itConf)));
			this.pipes.put("el", createEngine(GreekPipelineUD.getPipelineDescription(elConf)));

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
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);

		this.kafkaWriter = createEngine(createEngineDescription(AnalysisKafkaConsumer.class,
				AnalysisKafkaConsumer.PARAM_KAFKATOPIC,"TOP028_TEXT_ANALYSED",
				AnalysisKafkaConsumer.PARAM_KAFKABROKERS, kafkaBrokers,
				AnalysisKafkaConsumer.PARAM_KAFKAKEY, kafkaApiKey
				));

		initializePipelines();
	}

	public JCas runPipeline(JCas jcas) throws AnalysisEngineProcessException {
		// treat tweets differently
		boolean isTwitter = false;
		/*
		if ("TOP001_SOCIAL_MEDIA_TEXT".equals(topic)) {
			isTwitter = true;
		};
		 */

		// process with appropriate pipeline
		String lang = jcas.getDocumentLanguage();
		if (!pipes.containsKey(lang)) {
			logger.info("unknown language: "+lang);
			return null; // skip unknown languages
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
				return cleanView;
			} catch (CASException|AnalysisEngineProcessException e) {
				logger.warning(e.toString());
				throw new AnalysisEngineProcessException(e);
			}
		} else { // not Twitter
			this.pipes.get(lang).process(jcas);
			return jcas;
		}
	}

	@Override
	public void process(JCas kafkaCas) throws AnalysisEngineProcessException {

		String kafkaMessage = BeAwareMetaData.get(kafkaCas).getKafkaMessage();
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
				String filterApp = "$[?(@.body.description && @.header.actionType == 'Alert' && (@.header.sender == 'SCAPP' ||  @.header.sender == 'FRAPP'))]";
				List<String> matches = JsonPath.read(kafkaMessage, filterApp);
				if (matches.isEmpty()) {
					logger.info("rejected by filter: " + kafkaMessage);
					return;
				}
			}

			// build CAS for processing (like BeAwareKafkaIncidentReader)
			//JCas jcas = messageToCas(kafkaMessage);

			JCas resultCas = runPipeline(kafkaCas);
			/*String resultJson = AnalysisKafkaConsumer.extractJson(resultCas, "TOP028_TEXT_ANALYSED");
			System.out.println(resultJson);*/
			this.kafkaWriter.process(resultCas);

		} catch (Exception e) {
			logger.severe("skipping message:" + kafkaMessage);
			logger.severe(Throwables.getStackTraceAsString(e));
			throw new AnalysisEngineProcessException(e);
		}
	}

	/*@Deprecated
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
			metadata.setLanguage(jcas.getDocumentLanguage());
			metadata.addToIndexes();
			return jcas;
		} catch (UIMAException e1) {
			throw new AnalysisEngineProcessException(e1);
		}

	}*/

}
