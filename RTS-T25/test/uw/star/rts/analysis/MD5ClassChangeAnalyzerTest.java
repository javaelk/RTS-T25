package uw.star.rts.analysis;

import static org.junit.Assert.*;


import java.io.File;
import java.nio.file.Paths;

import org.junit.BeforeClass;
import org.junit.Test;

import uw.star.rts.artifact.*;

import uw.star.rts.extraction.ArtifactFactory;
import uw.star.rts.extraction.SIRJavaFactory;
import uw.star.rts.util.Constant;
import uw.star.rts.util.PropertyUtil;

import java.util.*;
public class MD5ClassChangeAnalyzerTest {

	static String EXPERIMENT_ROOT;
	static ArtifactFactory af;
	
	@BeforeClass
	public static void OneTimeSetup(){
		af =new SIRJavaFactory();
		EXPERIMENT_ROOT = PropertyUtil.getPropertyByName("config"+File.separator+"ARTSConfiguration.property",Constant.EXPERIMENTROOT);
		af.setExperimentRoot(EXPERIMENT_ROOT);		
	}
	
	@Test
	public void testMD5ClassChangeAnalyzer() {
		//todo:more tests here
		assertTrue(MD5ClassChangeAnalyzer.hasSameMD5Digest(
		Paths.get(EXPERIMENT_ROOT+"/apache-ant/versions.alt/orig/v1/ant/build/classes/org/apache/tools/ant/FileScanner.class"),
		Paths.get(EXPERIMENT_ROOT+"/apache-ant/versions.alt/orig/v0/ant/build/classes/org/apache/tools/ant/FileScanner.class")));
	}
	
	@Test
	public void diffTest(){
		Application testapp = af.extract("apache-ant");
		
		Program p0 = testapp.getProgram(ProgramVariant.orig, 0);
		Program p1= testapp.getProgram(ProgramVariant.orig, 1);
		CodeCoverageAnalyzer cca1 = new EmmaCodeCoverageAnalyzer(af,testapp,p0,testapp.getTestSuite());
		//this will populate statementeentities in source file, required before comparing two source files for line differences.
		//cca1.extractEntities(EntityType.SOURCE);
		cca1.extractEntities(EntityType.CLAZZ);
		
		CodeCoverageAnalyzer cca2 = new EmmaCodeCoverageAnalyzer(af,testapp,p1,testapp.getTestSuite());
		//this will populate statementeentities in source file, required before comparing two source files for line differences.
		//cca2.extractEntities(EntityType.SOURCE);
		cca2.extractEntities(EntityType.CLAZZ);
		
		List<? extends Entity> c0 = p0.getCodeEntities(EntityType.CLAZZ);
		System.out.println("\n\n===Total number of classes in Program 0 :" + c0.size());
		//for(Entity c: c0) System.out.println(((ClassEntity)c).getClassName());
		
		List<? extends Entity> c1 = p1.getCodeEntities(EntityType.CLAZZ);
		System.out.println("\n\n====Total number of classes in Program 1 :" + c1.size());
		//for(Entity c: c1)			System.out.println(((ClassEntity)c).getClassName());
		
		Map<String,List<ClassEntity>> resultMap = MD5ClassChangeAnalyzer.diff(p0, p1);
		List<ClassEntity> newEntities = resultMap.get("NEW");
		List<ClassEntity> modifiedEntities = resultMap.get("MODIFIED");
		List<ClassEntity> deletedEntities = resultMap.get("DELETED");
		assertTrue(newEntities.size()>0);
		System.out.println("=== number of new entities in v1 is "+ newEntities.size());
		//for(ClassEntity c: newEntities)			System.out.println(c.getClassName());
		
		assertTrue(modifiedEntities.size()>0);
		System.out.println("\n\n====number of modified entities in v0 is "+ modifiedEntities.size());
		for(ClassEntity c: modifiedEntities)			System.out.println(c.getPackageName()+"."+c.getClassName());
		
		assertTrue(deletedEntities.size()>0);

		System.out.println("\n\n====number of deletedEntities in v0 is "+ deletedEntities.size());
		//for(ClassEntity c: deletedEntities)			System.out.println(c.getClassName());
		
		Program p5 = testapp.getProgram(ProgramVariant.orig, 5);
		Program p6= testapp.getProgram(ProgramVariant.orig, 6);
		CodeCoverageAnalyzer cca5 = new EmmaCodeCoverageAnalyzer(af,testapp,p5,testapp.getTestSuite());
		//this will populate statementeentities in source file, required before comparing two source files for line differences.
		//cca1.extractEntities(EntityType.SOURCE);
		cca5.extractEntities(EntityType.CLAZZ);
		
		CodeCoverageAnalyzer cca6 = new EmmaCodeCoverageAnalyzer(af,testapp,p6,testapp.getTestSuite());
		//this will populate statementeentities in source file, required before comparing two source files for line differences.
		//cca2.extractEntities(EntityType.SOURCE);
		cca6.extractEntities(EntityType.CLAZZ);
		Map<String,List<ClassEntity>> resultMapv5v6 = MD5ClassChangeAnalyzer.diff(p5, p6);
		assertTrue(resultMapv5v6.keySet().size()>0);
		
	}
	@Test 
	public void bitsetTest(){
	      // create 2 bitsets
	      BitSet bitset1 = new BitSet(8);
	      BitSet bitset2 = new BitSet(8);

	      // assign values to bitset1
	      bitset1.set(0);
	      bitset1.set(1);
	      bitset1.set(2);
	      bitset1.set(3);
	      bitset1.set(4);
	      bitset1.set(5);

	      // assign values to bitset2
	      bitset2.set(2);
	      bitset2.set(4);
	      bitset2.set(6);
	      bitset2.set(8);
	      bitset2.set(10);

	      // print the sets
	      System.out.println("Bitset1:" + bitset1);
	      System.out.println("Bitset2:" + bitset2);

	      // print the first clear bit of bitset1
	      System.out.println("" + bitset1.nextClearBit(0));

	      // print the first clear bit of bitset2 after index 5
	      System.out.println("" + bitset2.nextClearBit(5));
	      System.out.println("" + bitset2.nextClearBit(7));
	      System.out.println("" + bitset2.nextClearBit(9));
	      System.out.println("" + bitset2.nextClearBit(10));
	}

}
