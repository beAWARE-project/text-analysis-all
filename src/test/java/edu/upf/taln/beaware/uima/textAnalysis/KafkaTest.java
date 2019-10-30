package edu.upf.taln.beaware.uima.textAnalysis;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiWriter;
import edu.upf.taln.beaware.kafka.BeawareCasUtils;

public class KafkaTest {

	@Test
	public void kafkaTestEN() throws IOException, UIMAException {
		JCas jCas = JCasFactory.createJCas();
		String inputFilePath = "src/test/resources/input/kafka/example_top002.json";
		String jsonString = FileUtils.readFileToString(new File(inputFilePath), "UTF8");
		BeawareCasUtils.initializeCAS(jCas.getCas(), jsonString);
		AnalysisEngine ae = AnalysisEngineFactory.createEngine(
				TextAnalysisRouter.class,
				TextAnalysisRouter.PARAM_KAFKABROKERS, ""/*,
				TextAnalysisRouter.PARAM_KAFKAKEY, "",
				TextAnalysisRouter.TARGET_VIEW, ""*/
				); 
		AnalysisEngine writer = AnalysisEngineFactory.createEngine(XmiWriter.class, 
				XmiWriter.PARAM_TARGET_LOCATION, "src/test/resources/results", 
				XmiWriter.PARAM_OVERWRITE, true,
				XmiWriter.PARAM_USE_DOCUMENT_ID, true,
				XmiWriter.PARAM_STRIP_EXTENSION, true);
		ae.process(jCas);
		writer.process(jCas);
	}
	
	@Test
	public void kafkaTestES() throws IOException, UIMAException {
		JCas jCas = JCasFactory.createJCas();
		String inputFilePath = "src/test/resources/input/kafka/example_top001.json";
		String jsonString = FileUtils.readFileToString(new File(inputFilePath), "UTF8");
		BeawareCasUtils.initializeCAS(jCas.getCas(), jsonString);
		AnalysisEngine ae = AnalysisEngineFactory.createEngine(
				TextAnalysisRouter.class,
				TextAnalysisRouter.PARAM_KAFKABROKERS, ""/*,
				TextAnalysisRouter.PARAM_KAFKAKEY, "",
				TextAnalysisRouter.TARGET_VIEW, ""*/
				); 
		AnalysisEngine writer = AnalysisEngineFactory.createEngine(XmiWriter.class, 
				XmiWriter.PARAM_TARGET_LOCATION, "src/test/resources/results", 
				XmiWriter.PARAM_OVERWRITE, true,
				XmiWriter.PARAM_USE_DOCUMENT_ID, true,
				XmiWriter.PARAM_STRIP_EXTENSION, true);
		ae.process(jCas);
		writer.process(jCas);
	}
}
