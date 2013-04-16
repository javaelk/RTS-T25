package uw.star.rts.technique;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import uw.star.rts.analysis.CodeCoverageAnalyzer;
import uw.star.rts.analysis.EmmaCodeCoverageAnalyzer;
import uw.star.rts.analysis.MD5ClassChangeAnalyzer;
import uw.star.rts.artifact.Application;
import uw.star.rts.artifact.ClassEntity;
import uw.star.rts.artifact.EntityType;
import uw.star.rts.artifact.Program;
import uw.star.rts.artifact.ProgramVariant;
import uw.star.rts.artifact.TestCase;
import uw.star.rts.artifact.TestSuite;
import uw.star.rts.artifact.TraceType;
import uw.star.rts.cost.CostFactor;
import uw.star.rts.cost.PrecisionPredictionModel;
import uw.star.rts.extraction.ArtifactFactory;
import uw.star.rts.extraction.SIRJavaFactory;
import uw.star.rts.util.Constant;
import uw.star.rts.util.PropertyUtil;
import uw.star.rts.util.StopWatch;
import org.apache.commons.collections.CollectionUtils;

public class ClassFirewallExtended2Test {

	static ClassFirewall_Extended2 classFirewalltech;
	static Program p,pPrime,v2;
	static TestSuite ts;
	static Application app;
	
	@BeforeClass
	public static void oneTimeSetUp() throws Exception {
		ArtifactFactory af =new SIRJavaFactory();
		af.setExperimentRoot(PropertyUtil.getPropertyByName("config"+File.separator+"ARTSConfiguration.property",Constant.EXPERIMENTROOT));
		app = af.extract("apache-xml-security",TraceType.CODECOVERAGE_EMMA);

		p=app.getProgram(ProgramVariant.orig, 0);
		pPrime=app.getProgram(ProgramVariant.orig, 1);
		v2= app.getProgram(ProgramVariant.orig, 2);
		ts = app.getTestSuite();
		classFirewalltech = new ClassFirewall_Extended2();
		classFirewalltech.setApplication(app);
	}
	

	@Test
	public void testSelectTests() {
		List<TestCase> selectedTC = classFirewalltech.selectTests(p, pPrime,new StopWatch());
		System.out.println("=== test cases selected by ClassFirewall" + selectedTC.size() + " ==="); 
		System.out.println(selectedTC);
		assertEquals("size of selected test cases",13,selectedTC.size());
		int count =0;
		for(TestCase tc: selectedTC)
			if( tc.isApplicabletoVersion(1))
			 count ++;
		System.out.println("selected test cases applicable to version 1 :" + count);
		assertEquals("size of test cases applicable to v1",15,ts.getTestCaseByVersion(1).size());
		
		selectedTC = classFirewalltech.selectTests(pPrime,v2,new StopWatch());
		System.out.println("=== test cases selected by ClassFirewall" + selectedTC.size() + " ==="); 
		System.out.println(selectedTC);
		assertEquals("size of selected test cases",13,selectedTC.size());
		count =0;
		for(TestCase tc: selectedTC)
			if( tc.isApplicabletoVersion(2))
			 count ++;
		System.out.println("selected test cases applicable to version 2 :" + count);
		assertEquals("size of test cases applicable to v2",15,ts.getTestCaseByVersion(2).size());
	}
	

	@Test
	public void testPredictPrecision() {
		assertEquals("test predict precision", 0.67,classFirewalltech.predictPrecision(PrecisionPredictionModel.RWPredictor,p,pPrime),0.01);
		assertEquals("test predict precision", 0.67,classFirewalltech.predictPrecision(PrecisionPredictionModel.RWPredictor_RegressionTestsOnly,p,pPrime),0.01);
		assertEquals("test predict precision", 0.94,classFirewalltech.predictPrecision(PrecisionPredictionModel.RWPredictor_multiChanges,p,pPrime),0.01);
	}
	
	@Test
	public void testCollectDepedentInformation(){
		CodeCoverageAnalyzer cca1 = new EmmaCodeCoverageAnalyzer(app.getRepository(),app,p,app.getTestSuite());
		cca1.extractEntities(EntityType.CLAZZ);
		CodeCoverageAnalyzer cca2 = new EmmaCodeCoverageAnalyzer(app.getRepository(),app,pPrime,app.getTestSuite());
		cca2.extractEntities(EntityType.CLAZZ);
		Map<String,List<ClassEntity>> md5DiffResults = MD5ClassChangeAnalyzer.diff(p, pPrime);
		List<ClassEntity> changedClasses = md5DiffResults.get(MD5ClassChangeAnalyzer.MODIFIED_CLASSENTITY_KEY);
		Map<ClassEntity,List<String>> dependentInfo_RecalculateForEachVersion = classFirewalltech.collectDependentInformation(p, changedClasses);
		ClassFirewall_Extended classFirewall_reuse = new ClassFirewall_Extended();
		classFirewall_reuse.setApplication(app);
		Map<ClassEntity,List<String>> dependentInfo_ReuseV0 = classFirewall_reuse.collectDependentInformation(p, changedClasses);
		//comparing dependentinfo 1 and 2 for v0
		compareDependentInfo(dependentInfo_RecalculateForEachVersion,dependentInfo_ReuseV0,changedClasses);
		//comparing dependentinfo 1 and 2 for v1
	
		CodeCoverageAnalyzer cca3 = new EmmaCodeCoverageAnalyzer(app.getRepository(),app,v2,app.getTestSuite());
		cca3.extractEntities(EntityType.CLAZZ);
		Map<String,List<ClassEntity>> md5DiffResults2 = MD5ClassChangeAnalyzer.diff(pPrime,v2);
		List<ClassEntity> changedClasses2 = md5DiffResults2.get(MD5ClassChangeAnalyzer.MODIFIED_CLASSENTITY_KEY);
		dependentInfo_RecalculateForEachVersion = classFirewalltech.collectDependentInformation(pPrime, changedClasses2);
		dependentInfo_ReuseV0 = classFirewall_reuse.collectDependentInformation(pPrime, changedClasses2);
		compareDependentInfo(dependentInfo_RecalculateForEachVersion,dependentInfo_ReuseV0,changedClasses2);

	}
	
	private void compareDependentInfo(Map<ClassEntity,List<String>> dependentInfo_RecalculateForEachVersion,Map<ClassEntity,List<String>> dependentInfo_ReuseV0,List<ClassEntity> changedClasses ) {
		assertTrue("both methods should have same set of class entities ", dependentInfo_ReuseV0.keySet().equals(dependentInfo_RecalculateForEachVersion.keySet()));
		for(ClassEntity c : changedClasses){
			if(CollectionUtils.isEqualCollection(dependentInfo_ReuseV0.get(c),dependentInfo_RecalculateForEachVersion.get(c))){
				System.out.println("dependentInfo_ReuseV0 and dependentInfo_RecalculateForEachVersion produces the same set of dependent classes for changed class "+ c.getClassName());
			}else {
				
				Collection<String> intersec = CollectionUtils.intersection(dependentInfo_ReuseV0.get(c),dependentInfo_RecalculateForEachVersion.get(c));
				System.out.println(c.getClassName() + " : dependent classes produced from dependentInfo_ReuseV0 but not in dependentInfo_RecalculateForEachVersion :" + CollectionUtils.subtract(dependentInfo_ReuseV0.get(c), intersec));
				System.out.println(c.getClassName() + " : dependent classes produced from dependentInfo_RecalculateForEachVersion but not in dependentInfo_ReuseV0  :" + CollectionUtils.subtract(dependentInfo_RecalculateForEachVersion.get(c),intersec));
			}
		}
	}

}
