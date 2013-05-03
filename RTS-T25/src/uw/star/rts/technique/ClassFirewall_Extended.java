package uw.star.rts.technique;

import java.util.HashMap;
//import com.javamex.classmexer.MemoryUtil;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.*;

import uw.star.rts.analysis.DependencyAnalyzer_C2CInboundTransitive;
import uw.star.rts.artifact.Application;
import uw.star.rts.artifact.ClassEntity;
import uw.star.rts.artifact.CodeCoverage;
import uw.star.rts.artifact.Program;

public class ClassFirewall_Extended extends ClassFirewall {
	DependencyAnalyzer_C2CInboundTransitive dp;

	public ClassFirewall_Extended() {
		log = LoggerFactory.getLogger(ClassFirewall_Extended.class.getName());
	}

	@Override
	public void setApplication(Application app) {
		super.setApplication(app);
		dp = new DependencyAnalyzer_C2CInboundTransitive(); // this call will
															// clean up previous
															// files generated
															// by
															// DependencyFinder
															// tool
	}

	/**
	 * General procedure: 1. Collect code coverage data of v0 2. Compare MD5
	 * signatures to produce a list of changed classes between v0 and v1 3.
	 * Collect dependency data of changed classes between v0 and v1 4. For
	 * subsequent versions, Compare MD5 signatures to produce a list of changed
	 * classes between p and pPrime 5. if dependency data is already collect at
	 * step 3, use it. otherwise, collect 6. Extract test cases that use the
	 * change classes or use classes that are directly or transitively dependent
	 * on changed classes e.g. coverage and dependency only need to analyze on
	 * v0. this method is assumed to be called in sequence of program versions,
	 * i.e. call selectTests(v0,v1) first , then selectTests(v1,v2) etc...
	 * collect direct dependent and transitive dependent information of changed
	 * classes
	 */
	Map<ClassEntity, List<String>> collectDependentInformation(Program p,
			List<ClassEntity> changedClassesCurrentVer) {

		Map<ClassEntity, List<String>> dependentClassesMap = new HashMap<>();
		if (p.getVersionNo() == 0) {
			log.debug("version 0 - perform full analysis");
			dp.analyze(p);
//			log.debug("DependencyAnalyzer_C2CInboundTransitive dp deep memory usage is "
//					+ MemoryUtil.deepMemoryUsageOf(dp)
//					+ "  - this maybe the problem one as it is live is across entire application");
			dependentClassesv0v1 = dp
					.findDirectAndTransitiveInBoundDependentClasses(changedClassesCurrentVer);// keep for v0v1
//			log.debug("DependencyAnalyzer_C2CInboundTransitive dependentClassesv0v1 deep memory usage is "
//					+ MemoryUtil.deepMemoryUsageOf(dependentClassesv0v1)
//					+ " this should keep the same size over the course of all versions");
			dependentClassesMap = dependentClassesv0v1;
		} else {// find additional dependency information for classes changed
				// between p and pPrim but not changed between v0 and v1.
			Set<ClassEntity> changedClassesv0v1 = dependentClassesv0v1.keySet();
			boolean found = false;
			// iterate through every entity changed in current version
			for (ClassEntity current : changedClassesCurrentVer) {
				found = false;
				for (ClassEntity v0 : changedClassesv0v1) {
					if (current.getName().equals(v0.getName())) {// compare with
																	// each
																	// change
																	// classes
																	// in v0 by
																	// names
						// if found
						log.debug(current
								+ "has also changed in v0-v1, reuse dependency information");
						dependentClassesMap.put(current,
								dependentClassesv0v1.get(v0));// reuse
																// transitive
																// dependent
																// information
																// from v0,
//						log.debug("dependentClassesMap deep memory usage is "
//								+ MemoryUtil
//										.deepMemoryUsageOf(dependentClassesMap)
//								+ " this should only grow within the version as it should be gced after each version");
						found = true;
						break;
					}
				}
				if (!found) {
					log.debug(current + " is not found in changeClassv0v1");
					dependentClassesMap
							.put(current,
									dp.findDirectAndTransitiveInBoundDependentClasses(current
											.getName()));

//					log.debug("dependentClassesMap deep memory usage is "
//							+ MemoryUtil.deepMemoryUsageOf(dependentClassesMap)
//							+ " this should only grow within the version as it should be gced after each version");

				}
			}
		}
		return dependentClassesMap;
	}
	
	//merge dependent information into cc before pass to predictor,
    /**
     * for each entity e in coverage matrix
     *    find all entities dependents on e (inbound dependent)  - eDependents
     *    for each entity e' in eDependent
     *       merge test cases covers e' into e
     *    end of for
     *  end of for  
     */
	protected CodeCoverage<ClassEntity> combineDependecyInfoIntoCoverage(Program p, CodeCoverage<ClassEntity> cc){
		CodeCoverage<ClassEntity> ncc = cc;    
		dp.analyze(p);
        for(ClassEntity ce : cc.getColumns()){
        	List<String> eDependent = dp.findDirectAndTransitiveInBoundDependentClasses(ce.getName());
        	ncc = ncc.transform(ce,eDependent);//original CoverCoverage object is not modified, a new codeCoverage matrix is returned
        }
        return ncc;
	}
}
