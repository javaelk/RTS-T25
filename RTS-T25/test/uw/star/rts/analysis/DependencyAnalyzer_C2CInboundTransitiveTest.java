package uw.star.rts.analysis;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import uw.star.rts.artifact.Application;
import uw.star.rts.artifact.Program;
import uw.star.rts.artifact.ProgramVariant;
import uw.star.rts.artifact.TraceType;
import uw.star.rts.extraction.ArtifactFactory;
import uw.star.rts.extraction.SIRJavaFactory;
import uw.star.rts.util.Constant;
import uw.star.rts.util.PropertyUtil;

public class DependencyAnalyzer_C2CInboundTransitiveTest {

	static Program v2;
	static DependencyAnalyzer_C2CInboundTransitive da;
	
	@BeforeClass
	public static void oneTimeSetUp(){
		ArtifactFactory af =new SIRJavaFactory();
		af.setExperimentRoot(PropertyUtil.getPropertyByName("config"+File.separator+"ARTSConfiguration.property",Constant.EXPERIMENTROOT));
		Application app = af.extract("apache-ant",TraceType.CODECOVERAGE_EMMA);
		v2=app.getProgram(ProgramVariant.orig, 2);

	}

	@Test
	public void testTransitiveClosure(){
		//this test compares dependent class directly from c2c with transitiveDependenetClasses to see if there is any additional classes are included.
		DependencyAnalyzer_C2CInbound dpDirect = new DependencyAnalyzer_C2CInbound();
		da = new DependencyAnalyzer_C2CInboundTransitive();
		da.analyze(v2);
		dpDirect.analyze(v2);
		List<String> directDependentClasses = dpDirect.findDirectDependentClasses("org.apache.tools.ant.Task");
		System.out.println("directDependentClasses: [" + directDependentClasses.size() +"]"+ directDependentClasses);
		assertTrue(directDependentClasses.size()==105);
		
		List<String> transitiveDependenetClasses = da.findDirectAndTransitiveInBoundDependentClasses("org.apache.tools.ant.Task"); 
		System.out.println("transitiveDependenetClasses: " + transitiveDependenetClasses);

		int counter =0;
		System.out.println(" == transitiveDependenetClasses already exist in direct inbound dependent classes ==== ");
		for(String cls : transitiveDependenetClasses)
			if(directDependentClasses.contains(cls))
				System.out.println(++counter + " " + cls) ;
		assertTrue(directDependentClasses.size()==counter);

		counter=0;
		System.out.println("\n\n == transitiveDependenetClasses that does NOT exist in direct inbound dependent classes ==== ");
		for(String cls : transitiveDependenetClasses)
			if(!directDependentClasses.contains(cls))
				System.out.println(++ counter + " " + cls);
        assertTrue(counter>0);

        counter =0;
        
		System.out.println("\n\n == direct inbound dependent classes that does NOT exist in transitiveDependenetClasses ==== ");
		for(String cls : directDependentClasses)
			if(!transitiveDependenetClasses.contains(cls))
				System.out.println(++counter + " " + cls);
		assertTrue(counter==0);

	}
}
