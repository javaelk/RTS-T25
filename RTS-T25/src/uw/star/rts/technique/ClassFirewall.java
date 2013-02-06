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
import uw.star.rts.cost.PrecisionPredictionModel;
import uw.star.rts.cost.RWPrecisionPredictor;
import uw.star.rts.cost.RWPrecisionPredictor2;
import uw.star.rts.cost.RWPrecisionPredictor_multiChanges;
import org.apache.commons.collections.CollectionUtils;

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
			CodeCoverageAnalyzer cca1 = new JacocoCodeCoverageAnalyzer(testapp.getRepository(),testapp,p,testapp.getTestSuite());
			cca1.extractEntities(EntityType.CLAZZ);
		}
		CodeCoverageAnalyzer cca2 = new JacocoCodeCoverageAnalyzer(testapp.getRepository(),testapp,pPrime,testapp.getTestSuite());
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
		Set<TestCase> selectedTests = new HashSet<TestCase>();		
		
		for(ClassEntity ce: dependentClassEntities)
			selectedTests.addAll(classCoverage.getLinkedEntitiesByColumn(ce));
		//log.debug("resultSet deep memory usage is "+MemoryUtil.deepMemoryUsageOf(resultSet) + " this should be really small and GCed");
		
		//only select tests that were exist in P and are still applicable to pPrime - i.e select from regression suite
		Set<TestCase> resultSet = new HashSet<>();
		for(TestCase tc: selectedTests)
			if(tc.isApplicabletoVersion(p.getVersionNo())&&tc.isApplicabletoVersion(pPrime.getVersionNo())){ 
				resultSet.add(tc);
			}else{
				log.debug("test case " + tc + " is selected, but does not applicable to version " + p.getVersionNo() + " and version " + pPrime.getVersionNo());
			}
		
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
		CodeCoverageAnalyzer cca1 = new JacocoCodeCoverageAnalyzer(testapp.getRepository(),testapp,p,testapp.getTestSuite());
		CodeCoverage<ClassEntity> classCoverage = cca1.createCodeCoverage(EntityType.CLAZZ);	
		return classCoverage;
	}
	/**
	 * current version is pPrime, previous version is p
	 * @return
	 */
	@Override
	public double predictPrecision(PrecisionPredictionModel pm,Program p,Program pPrime) {

		CodeCoverage<ClassEntity> cc = createCoverage(p);
		List<TestCase> regressionTests = testapp.getTestSuite().getRegressionTestCasesByVersion(p.getVersionNo());
        List<ClassEntity> regressionTestCoveredEntities = cc.getCoveredEntities(regressionTests);
        
		//merge dependent information into cc before pass to predictor,
        //this is the beginning of h3 experiment
        /**
         * for each entity e in coverage matrix
         *    find all entities dependents on e (inbound dependent)  - eDependents
         *    for each entity e' in eDependent
         *       merge test cases covers e' into e
         *    end of for
         *  end of for  
         */
        DependencyAnalyzer_C2CInboundTransitive dp = new DependencyAnalyzer_C2CInboundTransitive();
        for(ClassEntity ce : cc.getColumns()){
        	List<String> eDependent = dp.findDirectAndTransitiveInBoundDependentClasses(ce.getName());
        	cc.transform(ce,eDependent);
        }
        //this is the end of h3 experiment
        
		switch(pm){
		case RWPredictor:
			return RWPrecisionPredictor.predictSelectionRate(cc, testapp.getTestSuite().getTestCaseByVersion(p.getVersionNo()));

		case RWPredictorRegression:
			return RWPrecisionPredictor2.predictSelectionRate(cc,regressionTests);
		
		case RWPrecisionPredictor_multiChanges:
		//this prediction model would need to know nueDependentEntitiesmber of changed covered classes (within covered entities)
		    return RWPrecisionPredictor_multiChanges.predictSelectionRate(regressionTestCoveredEntities.size(), getModifiedCoveredClassEntities(regressionTestCoveredEntities,p,pPrime).size()); 	
			
		default:
        	log.error("unknow Precision Prediction Model : " + pm);     	
		}
		return Double.MIN_VALUE;
	}

	/**
	 * this one is for evaluation purpose and it automatically make predictions on all prediction models 
	 * this one has better performance than calling predictPrecision(PrecisionPredictionModel pm,Program p,Program pPrime) 3 times.
	 * @param p
	 * @param pPrime
	 * @return
	 */
	@Override
	public Map<PrecisionPredictionModel,Double> predictPrecision(Program p,Program pPrime) {

		Map<PrecisionPredictionModel,Double> results = new HashMap<>();

		CodeCoverage<ClassEntity> cc = createCoverage(p);
		List<TestCase> regressionTests = testapp.getTestSuite().getRegressionTestCasesByVersion(p.getVersionNo());
		List<ClassEntity> regressionTestCoveredEntities = cc.getCoveredEntities(regressionTests);
		
		//merge dependent information into cc before pass to predictor,
        //this is the beginning of h3 experiment
        /**
         * for each entity e in coverage matrix
         *    find all entities dependents on e (inbound dependent)  - eDependents
         *    for each entity e' in eDependent
         *       merge test cases covers e' into e
         *    end of for
         *  end of for  
         */
        DependencyAnalyzer_C2CInboundTransitive dp = new DependencyAnalyzer_C2CInboundTransitive();
        for(ClassEntity ce : cc.getColumns()){
        	List<String> eDependent = dp.findDirectAndTransitiveInBoundDependentClasses(ce.getName());
        	cc.transform(ce,eDependent);
        }
        //this is the end of h3 experiment

		for(PrecisionPredictionModel pm: PrecisionPredictionModel.values()){

			switch(pm){
			case RWPredictor:
				results.put(PrecisionPredictionModel.RWPredictor, RWPrecisionPredictor.predictSelectionRate(cc, testapp.getTestSuite().getTestCaseByVersion(p.getVersionNo())));
				break;

			case RWPredictorRegression:
				results.put(PrecisionPredictionModel.RWPredictorRegression,RWPrecisionPredictor2.predictSelectionRate(cc,regressionTests));
				break;

			case RWPrecisionPredictor_multiChanges:
				//this prediction model would need to know number of changed covered classes (within covered entities)
				results.put(PrecisionPredictionModel.RWPrecisionPredictor_multiChanges,RWPrecisionPredictor_multiChanges.predictSelectionRate(regressionTestCoveredEntities.size(), getModifiedCoveredClassEntities(regressionTestCoveredEntities,p,pPrime).size()));
				break;

			default:
				log.error("unknow Precision Prediction Model : " + pm);     	
			}
		}
		return results;

	}
	protected Collection<ClassEntity> getModifiedCoveredClassEntities(List<ClassEntity> coveredEntities,Program p, Program pPrime){
/*	TODO: already extracted here?	need a way to know if it's already available.
 * CodeCoverageAnalyzer cca1 = new EmmaCodeCoverageAnalyzer(testapp.getRepository(),testapp,p,testapp.getTestSuite());
		cca1.extractEntities(EntityType.CLAZZ);*/
		CodeCoverageAnalyzer cca2 = new JacocoCodeCoverageAnalyzer(testapp.getRepository(),testapp,pPrime,testapp.getTestSuite());
		cca2.extractEntities(EntityType.CLAZZ);
		
		// find all modified class entities
		Map<String,List<ClassEntity>> md5DiffResults = MD5ClassChangeAnalyzer.diff(p, pPrime);
		List<ClassEntity> changedClasses = md5DiffResults.get(MD5ClassChangeAnalyzer.MODIFIED_CLASSENTITY_KEY);
		//intersection of the two is the covered entities that are modified
		return  CollectionUtils.intersection(coveredEntities, changedClasses);
	}
	
	/**
	 * For this technique implementation, the total analysis cost is dominant by the cost of finding all transitive dependent classes of changed classes.
	 * So it should be proportional to number of changed classes and complexity of the code. 
	 * TODO: track actuals for all programs , correlate #of changes to cost. 
	 */
	@Override
	public long predictAnalysisCost() {

		// TODO Auto-generated method stub
		return 0;
	}
    
	
}
