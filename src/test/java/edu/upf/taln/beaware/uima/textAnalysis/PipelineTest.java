package edu.upf.taln.beaware.uima.textAnalysis;

import java.io.File;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.io.text.TextReader;
import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiWriter;

public class PipelineTest {
	
	
	public void testBeawarePipeline(String inputFolder, String inputFile, String language) throws ResourceInitializationException, AnalysisEngineProcessException {
		TextAnalysisRouter tar= new TextAnalysisRouter();
		try {
			tar.initializePipelines();
		} catch (ResourceInitializationException e) {
			e.printStackTrace();
		}
		
		CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
				TextReader.class,
				TextReader.PARAM_SOURCE_LOCATION, inputFolder,
				TextReader.PARAM_LANGUAGE, language,
				TextReader.PARAM_PATTERNS, inputFile);
		
		AnalysisEngine writer = AnalysisEngineFactory.createEngine(
				XmiWriter.class,
				XmiWriter.PARAM_TARGET_LOCATION, "/home/ivan/misc/beaware/tests/text-analysis-all",
				XmiWriter.PARAM_OVERWRITE, true);
		
		for(JCas jcas : SimplePipeline.iteratePipeline(reader)){
			tar.runPipeline(jcas);
			writer.process(jcas);
		}
	}
	
	@Test
	public void testEn() throws AnalysisEngineProcessException, ResourceInitializationException {
		String testFilePath = "src/test/resources/short/testFile.txt";
		File testFile = new File(testFilePath);
		String inputFolderPath = testFile.getParentFile().getAbsolutePath();
		String pattern = testFile.getName();
		testBeawarePipeline(inputFolderPath, pattern, "en");
	}
}
