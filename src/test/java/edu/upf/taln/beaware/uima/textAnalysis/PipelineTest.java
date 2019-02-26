package edu.upf.taln.beaware.uima.textAnalysis;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import de.tudarmstadt.ukp.dkpro.core.io.text.TextReader;
import edu.upf.taln.beaware.commons.extractor.Entity;
import edu.upf.taln.beaware.extractor.EntityExtractor;
import edu.upf.taln.beaware.extractor.RelationExtractor;

public class PipelineTest {
	
	
	public void testBeawarePipeline(String inputFolder, String inputFile, String language, String output) throws Exception {
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
		
		/*AnalysisEngine writer = AnalysisEngineFactory.createEngine(
				XmiWriter.class,
				XmiWriter.PARAM_TARGET_LOCATION, output,
				XmiWriter.PARAM_OVERWRITE, true);*/
		
		int i = 1;
		for (JCas jcas : SimplePipeline.iteratePipeline(reader)) {
			tar.runPipeline(jcas);
			
			String viewName = jcas.getViewName();
	        JCas view = jcas.getView(viewName);
			Map<String, Entity> dsynt_tree = EntityExtractor.extract(view);
	        Map<String, Entity> simplified_tree = RelationExtractor.extract(dsynt_tree);
	        HashMap<String, Object> body = new HashMap<String, Object>();
	        body.put("data", simplified_tree);
	        Map<String, Object> response = new HashMap<String,Object>();
	        response.put("body", body);
	        
	        ObjectMapper om = new ObjectMapper();
	        om.enable(SerializationFeature.INDENT_OUTPUT);
			String jsonOutput = om.writeValueAsString(response);
	        
	        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(output + "/" + language + i + ".json"))) {
	            writer.write(jsonOutput);
	        }
	        
	        i++;
		}
	}
	
	/*@Test
	public void testItTweets() throws Exception {
		String testFilePath = "/home/ivan/misc/beaware/tweetsTest/it/analysis2/it.txt";
		File testFile = new File(testFilePath);
		String inputFolderPath = testFile.getParentFile().getAbsolutePath();
		String pattern = "*.txt"; //testFile.getName();
		testBeawarePipeline(inputFolderPath, pattern, "it", "/home/ivan/misc/beaware/tweetsTest/it/analysis2/result2");
	}
	
	@Test
	public void testEnTweets() throws Exception {
		String testFilePath = "/home/ivan/misc/beaware/tweetsTest/en/analysis2/en.txt";
		File testFile = new File(testFilePath);
		String inputFolderPath = testFile.getParentFile().getAbsolutePath();
		String pattern = "*.txt"; //testFile.getName();
		testBeawarePipeline(inputFolderPath, pattern, "en", "/home/ivan/misc/beaware/tweetsTest/en/analysis2/result2");
	}
	
	@Test
	public void testEn() throws Exception {
		String testFilePath = "src/test/resources/short/testFile.txt";
		File testFile = new File(testFilePath);
		String inputFolderPath = testFile.getParentFile().getAbsolutePath();
		String pattern = testFile.getName();
		testBeawarePipeline(inputFolderPath, pattern, "en", "/home/ivan/misc/beaware/tests/text-analysis-all");
	}*/
}
