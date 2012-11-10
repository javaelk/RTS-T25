package uw.star.rts.technique;

import static org.junit.Assert.*;
import com.javamex.classmexer.MemoryUtil;

import java.io.File;
import java.util.List;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import uw.star.rts.artifact.Application;
import uw.star.rts.artifact.Program;
import uw.star.rts.artifact.ProgramVariant;
import uw.star.rts.artifact.TestCase;
import uw.star.rts.artifact.TestSuite;
import uw.star.rts.extraction.ArtifactFactory;
import uw.star.rts.extraction.SIRJavaFactory;
import uw.star.rts.util.Constant;
import uw.star.rts.util.PropertyUtil;
import uw.star.rts.util.StopWatch;

public class ClassFirewallExtendedTest{



	static uw.star.rts.technique.Technique classFirewalltech;
	static Program p,pPrime,v2;
	static TestSuite ts;
	static Application app;
	
	@BeforeClass
	public static void oneTimeSetUp() throws Exception {
		ArtifactFactory af =new SIRJavaFactory();
		af.setExperimentRoot(PropertyUtil.getPropertyByName("config"+File.separator+"ARTSConfiguration.property",Constant.EXPERIMENTROOT));
		app = af.extract("apache-xml-security");

		p=app.getProgram(ProgramVariant.orig, 0);
		pPrime=app.getProgram(ProgramVariant.orig, 1);
		v2= app.getProgram(ProgramVariant.orig, 2);
		ts = app.getTestSuite();
		classFirewalltech = new ClassFirewall_Extended();
		classFirewalltech.setApplication(app);
	}

	@Test
	public void testSelectTests3() {
	ArtifactFactory afactory =new SIRJavaFactory();
	afactory.setExperimentRoot(PropertyUtil.getPropertyByName("config"+File.separator+"ARTSConfiguration.property",Constant.EXPERIMENTROOT));
	Application testapp = afactory.extract("apache-ant");
	Program v0=testapp.getProgram(ProgramVariant.orig, 0);
	Program v1=testapp.getProgram(ProgramVariant.orig, 1);
	uw.star.rts.technique.Technique classFirewalltech1 = new ClassFirewall_Extended();
	classFirewalltech1.setApplication(testapp);
	List<TestCase> selectedTC = classFirewalltech1.selectTests(v0, v1,new StopWatch());
	selectedTC = classFirewalltech1.selectTests(v0, v1,new StopWatch());
	System.out.println("=== test cases selected by ClassFirewall for v6 : " + selectedTC.size() + " ==="); 
	System.out.println(selectedTC);
	assertEquals("size of selected test cases",28,selectedTC.size());
	int count =0;
	for(TestCase tc: selectedTC)
		if( tc.isApplicabletoVersion(1))
			count ++;
	System.out.println("selected test cases applicable to version 1 :" + count);
	assertEquals("Total number of test cases applicable to v1 ",34,testapp.getTestSuite().getTestCaseByVersion(1).size());
	}
	@Test
	public void testSelectTests2() {
	ArtifactFactory afactory =new SIRJavaFactory();
	afactory.setExperimentRoot(PropertyUtil.getPropertyByName("config"+File.separator+"ARTSConfiguration.property",Constant.EXPERIMENTROOT));
	Application testapp = afactory.extract("apache-ant");
	Program v0=testapp.getProgram(ProgramVariant.orig, 0);
	Program v1=testapp.getProgram(ProgramVariant.orig, 1);
	Program v5=testapp.getProgram(ProgramVariant.orig, 5);
	Program v6=testapp.getProgram(ProgramVariant.orig, 6);
	uw.star.rts.technique.Technique classFirewalltech1 = new ClassFirewall_Extended();
	//System.out.println("ClassFirewall_Extended deep memory usage: 1- after constructor call: "+ MemoryUtil.deepMemoryUsageOf(classFirewalltech1));
	classFirewalltech1.setApplication(testapp);
	
	//System.out.println("ClassFirewall_Extended deep memory usage: 2- after set application: " + MemoryUtil.deepMemoryUsageOf(classFirewalltech1));
	List<TestCase> selectedTC = classFirewalltech1.selectTests(v0, v1,new StopWatch());
	//System.out.println("ClassFirewall_Extended deep memory usage: 3- after select test of v0v1: " + MemoryUtil.deepMemoryUsageOf(classFirewalltech1));
	selectedTC = classFirewalltech1.selectTests(v5, v6,new StopWatch());
	//System.out.println("ClassFirewall_Extended deep memory usage: 4- after select test of v5v6: " + MemoryUtil.deepMemoryUsageOf(classFirewalltech1));
	System.out.println("=== test cases selected by ClassFirewall for v6 : " + selectedTC.size() + " ==="); 
	System.out.println(selectedTC);
	assertEquals("size of selected test cases",21,selectedTC.size());
	int count =0;
	for(TestCase tc: selectedTC)
		if( tc.isApplicabletoVersion(6))
			count ++;
	System.out.println("selected test cases applicable to version 6 :" + count);
	assertEquals("Total number of test cases applicable to v6 ",105,testapp.getTestSuite().getTestCaseByVersion(6).size());
	}
	
	@Test
	public void testSelectTests() {
		List<TestCase> selectedTC = classFirewalltech.selectTests(p, pPrime,new StopWatch());
		System.out.println("=== test cases selected by ClassFirewall for v1" + selectedTC.size() + " ==="); 
		System.out.println(selectedTC);
		assertEquals("size of selected test cases",13,selectedTC.size());
		int count =0;
		for(TestCase tc: selectedTC)
			if( tc.isApplicabletoVersion(1))
			 count ++;
		System.out.println("selected test cases applicable to version 1 :" + count);
		assertEquals("Total number of test cases applicable to v1",15,ts.getTestCaseByVersion(1).size());
		
		selectedTC = classFirewalltech.selectTests(pPrime,v2,new StopWatch());
		System.out.println("=== test cases selected by ClassFirewall for v2" + selectedTC.size() + " ==="); 
		System.out.println(selectedTC);
		assertEquals("size of selected test cases",13,selectedTC.size());
		count =0;
		for(TestCase tc: selectedTC)
			if( tc.isApplicabletoVersion(2))
			 count ++;
		System.out.println("selected test cases applicable to version 2 :" + count);
		assertEquals("Total number of test cases applicable to v2",15,ts.getTestCaseByVersion(2).size());

	}


	
	@Test
	public void testPredictPrecision() {
		assertEquals("test predict precision", 0.68,classFirewalltech.predictPrecision(),0.01);
	}

}
