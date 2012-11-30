package uw.star.rts.technique;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;

import uw.star.rts.artifact.Application;
import uw.star.rts.artifact.Program;
import uw.star.rts.artifact.ProgramVariant;
import uw.star.rts.artifact.TestCase;
import uw.star.rts.artifact.TestSuite;
import uw.star.rts.cost.PrecisionPredictionModel;
import uw.star.rts.extraction.ArtifactFactory;
import uw.star.rts.extraction.SIRJavaFactory;
import uw.star.rts.util.Constant;
import uw.star.rts.util.PropertyUtil;
import uw.star.rts.util.StopWatch;

public class ClassFirewallDirectTest {
	static uw.star.rts.technique.Technique classFirewalltech;
	static Program p,pPrime,v2;
	static TestSuite ts;
	
	@BeforeClass
	public static void oneTimeSetUp() throws Exception {
		ArtifactFactory af =new SIRJavaFactory();
		af.setExperimentRoot(PropertyUtil.getPropertyByName("config"+File.separator+"ARTSConfiguration.property",Constant.EXPERIMENTROOT));
		Application app = af.extract("apache-xml-security");

		p=app.getProgram(ProgramVariant.orig, 0);
		pPrime=app.getProgram(ProgramVariant.orig, 1);
		v2= app.getProgram(ProgramVariant.orig, 2);
		ts = app.getTestSuite();
		classFirewalltech = new ClassFirewall_Direct();
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
		assertEquals("test predict precision", 0.68,classFirewalltech.predictPrecision(PrecisionPredictionModel.RWPredictor),0.11);
	}

}