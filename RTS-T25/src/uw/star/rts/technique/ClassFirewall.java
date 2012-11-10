package uw.star.rts.technique;


import java.util.*;
//import com.javamex.classmexer.MemoryUtil;
import org.slf4j.Logger;
import uw.star.rts.artifact.Application;
import uw.star.rts.artifact.ClassEntity;
import uw.star.rts.artifact.CodeCoverage;
import uw.star.rts.artifact.EntityType;
import uw.star.rts.artifact.Program;
import uw.star.rts.artifact.ProgramVariant;
import uw.star.rts.artifact.TestCase;
import uw.star.rts.util.StopWatch;
import uw.star.rts.analysis.*;
import uw.star.rts.cost.CostFactor;
import uw.star.rts.cost.RWPrecisionPredictor;
public abstract class ClassFirewall extends Technique {

	Application testapp;

	Map<ClassEntity,List<String>> dependentClassesv0v1;
	
	CodeCoverage<ClassEntity> classCoverage;
	Logger log;
	Program p0;
	
	@Override
	public void setApplication(Application app) {
		testapp = app;
		//reset for each new application
		dependentClassesv0v1=null;   //  a map of change classes and their dependent classes
		classCoverage = null;
		p0=null;
	}

    
	@Override
	public List<TestCase> selectTests(Program p, Program pPrime, StopWatch stopwatch) {

		
		stopwatch.start(CostFactor.CoverageAnalysisCost);
        
		if(p.getVersionNo()==0){//Collect code coverage data of v0
			log.info("//1. collect code coverage data of v0");
			classCoverage = createCoverage(p);
			//log.debug("classCoverage for v0 deep memory usage is "+MemoryUtil.deepMemoryUsageOf(classCoverage) + " this should keep the same size over the course of all versions");
			p0=p;   //keep p0
			//log.debug("p0 deep memory usage is "+MemoryUtil.deepMemoryUsageOf(p0) + " this should keep the same size over the course of all versions");
		}else{//just extract all class entities of p and pPrime before comparison
			log.info("extract class entities");
			CodeCoverageAnalyzer cca1 = new EmmaCodeCoverageAnalyzer(testapp.getRepository(),testapp,p,testapp.getTestSuite());
			cca1.extractEntities(EntityType.CLAZZ);
		}
		CodeCoverageAnalyzer cca2 = new EmmaCodeCoverageAnalyzer(testapp.getRepository(),testapp,pPrime,testapp.getTestSuite());
		cca2.extractEntities(EntityType.CLAZZ);
		
		stopwatch.stop(CostFactor.CoverageAnalysisCost);

		log.info("//2. compare MD5 signatures to produce a list of changed classes between v"+ p.getVersionNo()+" and v"+pPrime.getVersionNo());
		stopwatch.start(CostFactor.ChangeAnalysisCost);

		Map<String,List<ClassEntity>> md5DiffResults = MD5ClassChangeAnalyzer.diff(p, pPrime);
		//log.debug("md5DiffResults deep memory usage is "+MemoryUtil.deepMemoryUsageOf(md5DiffResults) + " this should be really small and GCed");
		List<ClassEntity> changedClasses = md5DiffResults.get(MD5ClassChangeAnalyzer.MODIFIED_CLASSENTITY_KEY);
		//log.debug("changedClasses deep memory usage is "+MemoryUtil.deepMemoryUsageOf(changedClasses) + " this should be really small and GCed");
		
		log.info("//3. collect dependency data of v" + p.getVersionNo());
		//TODO: dependent files(xml) are not cleaned from version to version. this could be a problem
		Map<ClassEntity,List<String>> dependentClassesMap = collectDependentInformation(p,changedClasses);
		//log.debug("dependentClassesMap deep memory usage is "+MemoryUtil.deepMemoryUsageOf(dependentClassesMap) + " this should be really small and GCed");
		
		/*TODO: would be better if this can be changed
		 * convert list of class name string to list of classEntity,
		 * Since Emma does not report interface etc. not all classes exists in classEntity
		 *  Also in order to use the test case traceability matrix buit in version 0, these entities have to the entities from program version 0
		 */
		Set<ClassEntity> dependentClassEntities = new HashSet<ClassEntity>();
		for(String str: merge(dependentClassesMap)){ // merge into a list of Strings
			//only add if class entity is found
			ClassEntity classEntityFound = (ClassEntity)p0.getEntityByName(EntityType.CLAZZ, str);
			if(classEntityFound!=null) dependentClassEntities.add(classEntityFound); //find classEntity by string name,need to use the p0 instead of current version of the program
		}
		//log.debug("dependentClassEntities deep memory usage is "+MemoryUtil.deepMemoryUsageOf(dependentClassEntities) + " this should be really small and GCed");
		stopwatch.stop(CostFactor.ChangeAnalysisCost);

		//4. extract test cases that use the change classes or use classes that are directly or transitively dependent on changed classes
		stopwatch.start(CostFactor.ApplyTechniqueCost);
		//select test cases that cover changed class entities
		Set<TestCase> resultSet = new HashSet<TestCase>();		
		for(ClassEntity ce: dependentClassEntities)
			resultSet.addAll(classCoverage.getLinkedEntitiesByColumn(ce));
		//log.debug("resultSet deep memory usage is "+MemoryUtil.deepMemoryUsageOf(resultSet) + " this should be really small and GCed");
		stopwatch.stop(CostFactor.ApplyTechniqueCost);
		return new ArrayList<TestCase>(resultSet);
	}
    
	/**
	 * subclasses implements this to extract dependent information on different levels 
	 * @param p
	 * @param changedClasses
	 * @return
	 */
	abstract Map<ClassEntity,List<String>> collectDependentInformation(Program p,List<ClassEntity> changedClasses);
	
	/**
	 * Merge the map into a set of unique class names
	 * @param dependentClasses
	 * @return
	 */
	private Set<String> merge(Map<ClassEntity,List<String>> dependentClasses){
		Set<String> resultSet = new HashSet<String>();
		for(ClassEntity c : dependentClasses.keySet())
			resultSet.addAll(dependentClasses.get(c));
		return resultSet;
	}

	/**
	 * Helper method as this will be called twice. once for predicting cost, once for actual selecting tests. 
	 */
	CodeCoverage<ClassEntity> createCoverage(Program p){
		CodeCoverageAnalyzer cca1 = new EmmaCodeCoverageAnalyzer(testapp.getRepository(),testapp,p,testapp.getTestSuite());
		//this will populate statement entities in source file
		cca1.extractEntities(EntityType.CLAZZ);
		CodeCoverage<ClassEntity> classCoverage = cca1.createCodeCoverage(EntityType.CLAZZ);	
		return classCoverage;
	}
	/**
	 * Precision should be predicted based on first version of the program. 
	 * Test Suite should only contains test applicable to the first version
	 * @return
	 */
	@Override
	public double predictPrecision() {
		Program p = testapp.getProgram(ProgramVariant.orig, 0);
		CodeCoverage<ClassEntity> cc = createCoverage(p);
		return RWPrecisionPredictor.getPredicatedPercetageOfTestCaseSelected(cc, testapp.getTestSuite().getTestCaseByVersion(0));
	}

	/**
	 * For this technique implementation, the total analysis cost is dominant by the cost of finding all transitive dependent classes of changed classes.
	 * So it should be proportional to number of changed classes and complexity of the code. 
	 * TODO: track actuals for all programs , corelate #of changes to cost. 
	 */
	@Override
	public long predictAnalysisCost() {

		// TODO Auto-generated method stub
		return 0;
	}

}
