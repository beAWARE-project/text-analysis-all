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
import edu.upf.taln.beaware.kafka.KafkaReader;

public class KafkaTest {

	@Test
	public void kafkaTest() throws IOException, UIMAException {
		JCas jCas = JCasFactory.createJCas();
		String inputFilePath = "src/test/resources/input/kafka/TOP001.json";
		String jsonString = FileUtils.readFileToString(new File(inputFilePath), "UTF8");
		KafkaReader.initializejCAS(jCas, jsonString);
		AnalysisEngine ae = AnalysisEngineFactory.createEngine(
				TextAnalysisRouter.class,
				TextAnalysisRouter.PARAM_KAFKABROKERS, ""/*,
				TextAnalysisRouter.PARAM_KAFKAKEY, "",
				TextAnalysisRouter.TARGET_VIEW, ""*/
				); 
		AnalysisEngine writer = AnalysisEngineFactory.createEngine(
				XmiWriter.class,
				XmiWriter.PARAM_OVERWRITE, true,
				XmiWriter.PARAM_TARGET_LOCATION, "src/test/resources/output/xmi/");
		ae.process(jCas);
		writer.process(jCas);
	}
}
