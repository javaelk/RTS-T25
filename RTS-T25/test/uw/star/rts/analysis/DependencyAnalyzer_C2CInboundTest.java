package uw.star.rts.analysis;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import uw.star.rts.artifact.Application;
import uw.star.rts.artifact.ClassEntity;
import uw.star.rts.artifact.Program;
import uw.star.rts.artifact.ProgramVariant;
import uw.star.rts.extraction.ArtifactFactory;
import uw.star.rts.extraction.SIRJavaFactory;
import uw.star.rts.util.Constant;
import uw.star.rts.util.PropertyUtil;

public class DependencyAnalyzer_C2CInboundTest {
	
	static Program v0,v1;
	static DependencyAnalyzer_C2CInbound da;
	
	@BeforeClass
	public static void oneTimeSetUp(){
		ArtifactFactory af =new SIRJavaFactory();
		af.setExperimentRoot(PropertyUtil.getPropertyByName("config"+File.separator+"ARTSConfiguration.property",Constant.EXPERIMENTROOT));
		Application app = af.extract("apache-xml-security");
		v0=app.getProgram(ProgramVariant.orig, 0);
		v1=app.getProgram(ProgramVariant.orig, 1);
		da = new DependencyAnalyzer_C2CInbound();
		da.analyze(v1);
	}

    @Test
	public void testFindInBoundDependentClassesClassEntity() {
		ClassEntity c = new ClassEntity(v1,"org.xml.sax","org.xml.sax.ErrorHandler",null);
		List<String> dependentClasses = da.findDirectDependentClasses(c);
		assertTrue("dependent on org.apache.xml.security.signature.XMLSignatureInput",dependentClasses.contains("org.apache.xml.security.signature.XMLSignatureInput"));
		assertTrue("have 11 dependents",dependentClasses.size()==11);

	}

    @Test
	public void testFindInBoundDependentClassesClassEntityList() {
		List<ClassEntity> ces = new ArrayList<ClassEntity>();
		ClassEntity c1 = new ClassEntity(v1,"org.xml.sax","ErrorHandler",null);
		ClassEntity c2 = new ClassEntity(v1,"org.xml.sax","EntityResolver",null);
		ClassEntity c3 = new ClassEntity(v1,"org.w3c.dom","Comment",null);
		ClassEntity c4 = new ClassEntity(v1,"org.apache.xml.security.signature","XMLSignatureInput",null);
		ces.add(c1);
		ces.add(c2);
		ces.add(c3);
		ces.add(c4);
		Map<ClassEntity,List<String>> dependentClasses = da.findDirectDependentClasses(ces);
		assertTrue("dependent on org.apache.xml.security.signature.XMLSignatureInput",dependentClasses.get(c1).contains("org.apache.xml.security.signature.XMLSignatureInput"));
		assertTrue(dependentClasses.get(c2).contains("org.apache.xml.security.test.c14n.implementations.Canonicalizer20010315Test"));
		assertTrue(dependentClasses.get(c3).contains("org.apache.xml.security.c14n.implementations.Canonicalizer20010315"));
		
		assertTrue(dependentClasses.get(c4).size()==48);
		assertTrue(dependentClasses.get(c4).contains("org.apache.xml.security.test.transforms.implementations.TransformBase64DecodeTest"));
	}

    @Test
	public void testFindInBoundDependentClassesClassEntityList2() {
		da = new DependencyAnalyzer_C2CInbound();
		da.analyze(v0);
		List<ClassEntity> ces = new ArrayList<ClassEntity>();
		ClassEntity c4 = new ClassEntity(v1,"org.apache.xml.security.signature","XMLSignatureInput",null);
		ces.add(c4);
		Map<ClassEntity,List<String>> dependentClasses = da.findDirectDependentClasses(ces);
		assertTrue(dependentClasses.get(c4).size()==37);
		assertTrue(dependentClasses.get(c4).contains("org.apache.xml.security.test.transforms.implementations.TransformBase64DecodeTest"));
	}
    
    @Test
	public void testFindInBoundDependentClassesString() {
		//Dependency dp = new Dependency(Paths.get("test/testfiles/result_c2cinbound.xml"));
		List<String> dependentClasses = da.findDirectDependentClasses("org.xml.sax.ErrorHandler");
		assertTrue("dependent on org.apache.xml.security.signature.XMLSignatureInput",dependentClasses.contains("org.apache.xml.security.signature.XMLSignatureInput"));
		assertTrue("have 11 dependents",dependentClasses.size()==11);
	}
}
