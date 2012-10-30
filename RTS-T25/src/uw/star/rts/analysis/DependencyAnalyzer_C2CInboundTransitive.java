package uw.star.rts.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.slf4j.LoggerFactory;

import uw.star.rts.analysis.jaxb.Dependencies;
import uw.star.rts.artifact.ClassEntity;

/**
 * Run dependency finder tool to analyze Class to class inbound dependency including transitively dependent classes and generate a XML file
 * @author wliu
 *
 */
public class DependencyAnalyzer_C2CInboundTransitive extends DependencyAnalyzer {

	String C2CINBOUNDTRANSITIVE_FILE_SUFFIX = "_C2CInboundTransitive";
    //Constructor
	public DependencyAnalyzer_C2CInboundTransitive(){
		log = LoggerFactory.getLogger(DependencyAnalyzer_C2CInboundTransitive.class.getName());
		getConfig();
		cleanup();  //remove any existing files in the output file directory
	}
	/**
	 * Call closure command with className as parameter then call c2c
	 * @param className
	 */
	public List<String> findDirectAndTransitiveInBoundDependentClasses(String className){
		//execute two DependencyFinder commands and generate a result file (suffix is classname) 
		Path closureFile = Paths.get(xmlfile.getParent().toString(),"closure"+C2CINBOUNDTRANSITIVE_FILE_SUFFIX+"."+className+".xml");
		CommandLine command2 = dependencyClosure(xmlfile,closureFile,className);

		Path finalOutput = Paths.get(outputFile.substring(0, outputFile.lastIndexOf("."))+C2CINBOUNDTRANSITIVE_FILE_SUFFIX+"."+className+".xml");
		CommandLine command3 = c2cInbound(closureFile,finalOutput);

		try{
			DependencyFinderResultHandler handler2 = runDependencyFinder(command2);
			handler2.waitFor();
			waitForFileCreated(closureFile);
			DependencyFinderResultHandler handler3 = runDependencyFinder(command3);
			handler3.waitFor();
			waitForFileCreated(finalOutput);
		}catch(IOException e){
			e.printStackTrace();
		}catch(InterruptedException e){
			e.printStackTrace();
		}
		//unmarshal the result xml file
		Dependencies dp = unmarshall(finalOutput);

		//return all class names in the result xml file , as this result xml file is for the given ClassEntity only
		List<String> resultLst = new ArrayList<String>();
		if(dp!=null)
			for(uw.star.rts.analysis.jaxb.Package p: dp.getPackage())
				for(uw.star.rts.analysis.jaxb.Class cls : p.getClazz())
					resultLst.add(cls.getName());
		return resultLst;
		
	}

	
	public Map<ClassEntity,List<String>> findDirectAndTransitiveInBoundDependentClasses(List<ClassEntity> ces){
		  Map<ClassEntity,List<String>> resultMap = new HashMap<ClassEntity,List<String>>();
		  for(ClassEntity c: ces)
			  resultMap.put(c, findDirectAndTransitiveInBoundDependentClasses(c.getClassName()));
		   
		  return resultMap;
		}
		
	
	/**
	 * sample command line : c2c closure.xml -xml -out result_c2cInboundLevel2_Task.xml 
	 */
	CommandLine c2cInbound(Path in,Path out){
		Map<String,File> map = new HashMap<String,File>();
		map.put("in", in.toFile());
		map.put("out", out.toFile());
		CommandLine commandline = new CommandLine(DEPENDENCYFINDER_HOME+File.separator+"bin"+File.separator+ "c2c");
		commandline.addArgument("${in}");
		commandline.addArgument("-xml");
		commandline.addArgument("-out");
		commandline.addArgument("${out}");
		commandline.setSubstitutionMap(map);
		return  commandline;
	}
	
	/**
	 * this method calls dependencyClosure command with -maximum-inbound-depth value as unbound
	 * i.e. find all immediate dependent classes
	 * @param in
	 * @param out
	 * @param className
	 * @param maxInboundDepth
	 * @return
	 */
	CommandLine dependencyClosure(Path in,Path out, String className){
		return this.dependencyClosure(in, out, className, "unbound");
	}
	/**
	 * @see http://depfind.sourceforge.net/Tools.html#DependencyClosure
	 * ##-maximum-inbound-depth If value is omitted, or if it is not a number, than the traversal is unbounded. If the switch is omitted, the traversal will be unbounded.
	 * ## maximum-outbound-depth - If value is zero (0), don't follow any outbound dependencies.
	 * DependencyClosure -xml -out closure.xml -maximum-outbound-depth 0 -maximum-inbound-depth 2 -start-includes /org.apache.tools.ant.Task/ df.xml
	 * @param 
	 * @param in
	 * @param out
	 * @return all direct dependent and transitive dependent class of the given class
	 */
	CommandLine dependencyClosure(Path in,Path out, String className,String maxInboundDepth){
		Map<String,File> map = new HashMap<String,File>();
		map.put("in", in.toFile());
		map.put("out", out.toFile());
		CommandLine commandline = new CommandLine(DEPENDENCYFINDER_HOME+File.separator+"bin"+File.separator+ "DependencyClosure");
		commandline.addArgument("-xml");
		commandline.addArgument("-out");
		commandline.addArgument("${out}");
		commandline.addArgument("-maximum-outbound-depth");
		commandline.addArgument("0");
		
		commandline.addArgument("-maximum-inbound-depth");
		commandline.addArgument(maxInboundDepth);
		commandline.addArgument("-start-includes");
		commandline.addArgument("/"+className+"/");
		commandline.addArgument("${in}");
		commandline.setSubstitutionMap(map);
		return  commandline;
	}
	
}
