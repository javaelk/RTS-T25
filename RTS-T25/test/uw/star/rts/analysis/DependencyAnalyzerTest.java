package uw.star.rts.analysis;

import static org.junit.Assert.*;


import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import org.junit.BeforeClass;
import org.junit.Test;

import uw.star.rts.analysis.DependencyAnalyzer.DependencyFinderResultHandler;
import uw.star.rts.artifact.*;
import uw.star.rts.extraction.ArtifactFactory;
import uw.star.rts.extraction.SIRJavaFactory;
import uw.star.rts.util.Constant;
import uw.star.rts.util.PropertyUtil;

public class DependencyAnalyzerTest {
     
	static String EXPERIMENT_ROOT;
	static ArtifactFactory af;
	
	@BeforeClass
	public static void OneTimeSetup(){
		af =new SIRJavaFactory();
		EXPERIMENT_ROOT = PropertyUtil.getPropertyByName("config"+File.separator+"ARTSConfiguration.property",Constant.EXPERIMENTROOT);
		af.setExperimentRoot(EXPERIMENT_ROOT);		
	}
	
	@Test
	public void testRunDependencyFinder() {
		DependencyAnalyzer da = new DependencyAnalyzer();
		Path classFilesRoot = Paths.get(EXPERIMENT_ROOT+"/apache-ant/versions.alt/orig/v2/ant/build/classes");
		try{
			DependencyFinderResultHandler handler = da.runDependencyFinder(da.extract(classFilesRoot,Paths.get(da.outputFile)));
			handler.waitFor();

		assertTrue("result file is created",Files.exists(Paths.get(da.outputFile)));
		assertTrue("result file size is greater than zero",Files.size((Paths.get(da.outputFile)))>0);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	@Test
	public void testAnalyze(){

		Application app = af.extract("apache-xml-security");
		Program v1=app.getProgram(ProgramVariant.orig, 1);
		DependencyAnalyzer da = new DependencyAnalyzer();
		da.analyze(v1);
		try{
		assertTrue("result file is created",Files.exists(Paths.get(da.outputFile)));
		assertTrue("result file size is greater than zero",Files.size((Paths.get(da.outputFile)))>0);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

}
