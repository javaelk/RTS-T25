package uw.star.rts.analysis;
import uw.star.rts.analysis.jaxb.Dependencies;
import java.io.InputStream;
import uw.star.rts.artifact.*;
import uw.star.rts.util.XMLJAXBUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import javax.xml.bind.JAXBException;

import org.apache.commons.exec.*;
import org.slf4j.LoggerFactory;
/**
 * This class provides class to class direct inbound dependency information only. It will run DependencyExtractor command from superclass, 
 * then run c2c command to reduce the graph to class level inbound  
 * @author wliu
 *
 */
public class DependencyAnalyzer_C2CInbound extends DependencyAnalyzer {

	String C2CINBOUND_FILE_SUFFIX = "_C2CInbound";
	Dependencies dp;
	
	public DependencyAnalyzer_C2CInbound(){
		log = LoggerFactory.getLogger(DependencyAnalyzer_C2CInbound.class.getName());
		getConfig();
		cleanup();
	}
	/**
	 * Run dependency finder tool to analyze Class to class inbound dependency and generate a XML file
	 * This method will call super to execute DependencyExtractor command first
	 * @param p - the program to be analyzed
	 * @return a Path pointing to the result XML file to represent all dependency information of the given program
	 */
	@Override
	public Path analyze(Program p){
		//WARNING: p.getCodeFilesRoot() currently returnS build directory of the application. It contains main class files as well as unit test classes in some cases
		//This should not however impact the test selection results as long as there is no EMMA code coverage information for unit test classes. Unit test classes
		//should have been excluded when collect code coverage information in EMMA.
		Path classFilesRootPath = p.getCodeFilesRoot(); //Dependency Finder tool works on binary class files 
		xmlfile = Paths.get(outputFile.substring(0, outputFile.lastIndexOf("."))+C2CINBOUND_FILE_SUFFIX+".xml");   //add suffix to result file
		Path extractionOutput = Paths.get(xmlfile.getParent().toString(),"df_C2CInbound.xml"); 
		//command1 output is in the same directory as outputFile , name is df.xml
		CommandLine command1 = extract(classFilesRootPath,extractionOutput); 
		CommandLine command2 = c2cInbound(extractionOutput,xmlfile);
		try{
			DependencyFinderResultHandler handler1 = runDependencyFinder(command1);
			handler1.waitFor();
			waitForFileCreated(extractionOutput);
			DependencyFinderResultHandler handler2 = runDependencyFinder(command2);
			handler2.waitFor();
			waitForFileCreated(xmlfile);
		}catch(IOException e){
			e.printStackTrace();
		}catch(InterruptedException e){
			e.printStackTrace();
		}
		return xmlfile;
	}
	/**
	 * sample command line : c2c df.xml  -show-inbounds -xml -out result_c2cInbound.xml 
	 */
	CommandLine c2cInbound(Path in,Path out){
		Map<String,File> map = new HashMap<String,File>();
		map.put("in", in.toFile());
		map.put("out", out.toFile());
		CommandLine commandline = new CommandLine(DEPENDENCYFINDER_HOME+File.separator+"bin"+File.separator+ "c2c");
		commandline.addArgument("${in}");
		commandline.addArgument("-show-inbounds");
		commandline.addArgument("-xml");
		commandline.addArgument("-out");
		commandline.addArgument("${out}");
		commandline.setSubstitutionMap(map);
		return  commandline;
	}
	/**
	 * Find all inbound dependencies of a given class
	 * @param c - a classentity object 
	 * @return all classes dependent on c
	 */
	public List<String> findDirectDependentClasses(ClassEntity c){
		List<String> resultLst = new ArrayList<String>();

		try(InputStream stream = Files.newInputStream(xmlfile)){
			dp=XMLJAXBUtil.unmarshall(Dependencies.class,stream);

			if(dp!=null){	
				for(uw.star.rts.analysis.jaxb.Package p: dp.getPackage()){
					//this is optimized to compare package name first then class name
					if(p.getName().equals(c.getPackageName())){
						for(uw.star.rts.analysis.jaxb.Class cls : p.getClazz()){
							if(c.getClassName().equals(cls.getName())){
								for(uw.star.rts.analysis.jaxb.Inbound ib : cls.getInbound())
									resultLst.add(ib.getContent());
								break;
							}
						}
					}
				}
			}else{
				log.error("context tree is still null after unmarshall the result xml file " + xmlfile.getFileName());
			}
		}catch(IOException e){
			log.error("IOException in reading file " + xmlfile.getFileName());
			e.printStackTrace();
		}catch(JAXBException e){
			log.error("JAXBException"+ xmlfile.getFileName());
			e.printStackTrace();
		}
		return resultLst;
	}


	/**
	 * Find all inbound dependencies of a given class
	 * @param c - fully qualified class name 
	 * @return all classes dependent on c
	 */
	public List<String> findDirectDependentClasses(String c){
		List<String> resultLst = new ArrayList<String>();
		//use JAXB to unmarshall XML doc if not already done. this would read the whole xml file into memory as a tree
		try(InputStream stream = Files.newInputStream(xmlfile)){
			dp=XMLJAXBUtil.unmarshall(Dependencies.class,stream);
			if(dp!=null){
				for(uw.star.rts.analysis.jaxb.Package p: dp.getPackage()){
					for(uw.star.rts.analysis.jaxb.Class cls : p.getClazz()){
						if(c.equals(cls.getName())){
							for(uw.star.rts.analysis.jaxb.Inbound ib : cls.getInbound())
								resultLst.add(ib.getContent());
							break;
						}
					}
				}
			}else{
				log.error("context tree is still null after unmarshall the result xml file " + xmlfile.getFileName());
			}	
		}catch(IOException e){
			log.error("IOException in reading file " + xmlfile.getFileName());
			e.printStackTrace();
		}catch(JAXBException e){
			log.error("JAXBException"+ xmlfile.getFileName());
			e.printStackTrace();
		}
		return resultLst;
	}

	/**
	 * Find all inbound dependencies of the given classes
	 * @param c - list of classentity objects 
	 * @return all classes dependent on c
	 */
	public Map<ClassEntity,List<String>> findDirectDependentClasses(List<ClassEntity> classEntities){
		Map<ClassEntity,List<String>> resultmap = new HashMap<ClassEntity,List<String>>();
		List<String> resultLst = new ArrayList<String>();
		try(InputStream stream = Files.newInputStream(xmlfile)){
			dp=XMLJAXBUtil.unmarshall(Dependencies.class,stream);
			if(dp!=null){
				for(uw.star.rts.analysis.jaxb.Package p: dp.getPackage()){
					log.debug("parsing package "+ p.getName());
					for(ClassEntity ce: classEntities){
						log.debug("Search dependent for class: package = " + ce.getPackageName() + " className=" +  ce.getClassName() );
						if(resultmap.containsKey(ce)) {
							log.debug("\tskip -- already found dependent classes for " + ce.getName() );
							break; //skip it if ce's inbound dependencies are already found
						}
						if(p.getName().equals(ce.getPackageName())){
							log.debug("\tsame package, search continues ...");
							for(uw.star.rts.analysis.jaxb.Class cls : p.getClazz()){
								if(ce.getName().equals(cls.getName())){//same package , same class
									log.debug("\t\t same package, same class, found! ");
									for(uw.star.rts.analysis.jaxb.Inbound ib : cls.getInbound())
										resultLst.add(ib.getContent());
									resultmap.put(ce, resultLst);
									resultLst=new ArrayList<>();  
									break;//same ce won't be the same package again, skip through
								}else{
									log.debug("\t\tclass name in xml "+ cls.getName() + " is different from " + ce.getName() );
								}
							}
						}else{
							log.debug("\tdifferent package, search next");
						}
					}
				}
			}else{
				log.error("context tree is still null after unmarshall the result xml file " + xmlfile.getFileName());
			}
		}catch(IOException e){
			log.error("IOException in reading file " + xmlfile.getFileName());
			e.printStackTrace();
		}catch(JAXBException e){
			log.error("JAXBException"+ xmlfile.getFileName());
			e.printStackTrace();
		}
		return resultmap;
	}

}
