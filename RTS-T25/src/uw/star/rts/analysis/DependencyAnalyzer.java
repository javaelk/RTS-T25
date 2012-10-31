package uw.star.rts.analysis;
import uw.star.rts.analysis.jaxb.Dependencies;
import uw.star.rts.artifact.*;
import uw.star.rts.util.FileUtility;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;

import org.slf4j.*;
import org.apache.commons.exec.*;
import org.apache.commons.exec.environment.*;

/**
 * This class analyzes code dependency. It is an API wrapper of Dependency finder tool 
 * @see http://depfind.sourceforge.net/
 * The Dependency finder tool analyzes binary Java code and create a Dependency class which is a wrapper of result xml file.
 * This class calls Dependency Finder tool directly. Executing external processes from Java is a well-known problem area.
 * This class uses Apache Commons Exec package to handle all the details of calling an external process from Java.
 * @see http://commons.apache.org/exec/
 * 
 * Current implementation uses JAXB2.0 @SEE http://docs.oracle.com/javase/tutorial/jaxb/intro/index.html
 * and will read the entire result xml file into memory. However, if memory usage becomes a problem, this can be refactored using 
 * STAX @http://docs.oracle.com/javase/tutorial/jaxb/intro/index.html
 * or woodstox @http://woodstox.codehaus.org/ 
 * @author Weining Liu
 *
 */
public class DependencyAnalyzer {
	String CONFIGFILE = "config"+File.separator+"RTS_T25.property";
	
	Path xmlfile; //this is the result xml file after analyze() method call.
	
    String dependencyFinderCommand;
    String outputFile;
    int jobTimeout;
    String JAVA_HOME;
    String DEPENDENCYFINDER_HOME;
    String DEPENDENCYFINDER_OPTS;
    
    Logger log;
    
    //Constructor
	public DependencyAnalyzer(){
		log = LoggerFactory.getLogger(DependencyAnalyzer.class.getName());
		getConfig();
		cleanup();  //remove any existing files in the output file directory
	}

	/**
	 * Run dependency finder tool to analyze dependency and generate a XML file
	 * @param p - the program to be analyzed
	 * @return a Path pointing to the result XML file to represent all dependency information of the given program
	 */
   public Path analyze(Program p){
		//find class files
		Path classFilesRootPath = p.getCodeFilesRoot(); //Dependency Finder tool works on binary class files .
		CommandLine command = extract(classFilesRootPath,Paths.get(outputFile));
		try{
			DependencyFinderResultHandler handler = runDependencyFinder(command);
			handler.waitFor();
		}catch(IOException e){
			e.printStackTrace();
		}catch(InterruptedException e){
			e.printStackTrace();
		}
		xmlfile =Paths.get(outputFile); 
		//test if result file is actually created.
		waitForFileCreated(xmlfile);
		return xmlfile;
    }
	
   /**
    * wait until xmlfile is created and writeable
    * @param xmlfile
    * @return
    */
   boolean waitForFileCreated(Path xmlfile){

	   //scenario 1 - already created
	   if(Files.exists(xmlfile)&&Files.isWritable(xmlfile))
		   return true;

	   //scenario2 - does not exist or not readable
	   int counter =0;
	   do{
		   log.error("dependencey finder process has completed but result file "+ xmlfile.getFileName().toString() + " not yet exist! Wait for 5 seconds");
		   try {
			   Thread.sleep(5000L);
		   } catch (InterruptedException e) {
			   e.printStackTrace();
		   }
		  if(Files.exists(xmlfile)&&Files.isWritable(xmlfile))
			   return true;
		   counter++;
	   }while(counter <= 6);

	   //scenario 3 : still not exist and readable after 30 second, throw exception
	   log.error("dependencey finder process has completed but result file "+ xmlfile.getFileName().toString() + " does not exist after 30 seconds");
	   throw new MissingResourceException(xmlfile+ "does not exist and readable","DependencyAnalyzer",xmlfile.toString());
   }
   
   
   /**
    * This will delete all files in the output file directory
    * @return
    */
   public boolean cleanup(){
	   Path outputDir = Paths.get(outputFile).getParent();
	   for(Path f: FileUtility.listFiles(outputDir)){
		   try{
			   Files.delete(f);
		   } catch (NoSuchFileException x) {
			   System.err.format("%s: no such" + " file or directory%n", f);
		   } catch (DirectoryNotEmptyException x) {
			   System.err.format("%s not empty%n", f);
		   } catch (IOException x) {
			   // File permission problems are caught here.
			   System.err.println(x);
		   }
	   }

	   if(outputDir.toFile().list().length==0)
		   return true;
	   return false;
   }
	/**
	 * Run Dependency Finder tool with given command line parameters. 
	 */
	DependencyFinderResultHandler runDependencyFinder(CommandLine command)
	throws IOException{
        // create the executor and consider the exitValue '1' as success
        Executor executor = new DefaultExecutor();
        executor.setExitValue(0);
        
        // create a watchdog if requested
        if(jobTimeout > 0) {
        	ExecuteWatchdog watchdog = new ExecuteWatchdog(jobTimeout);
            executor.setWatchdog(watchdog);
        }

	   log.debug("Executing command " + command.toString());
       int exitValue = executor.execute(command,setEnvironmentVariables());
       DependencyFinderResultHandler resultHandler = new DependencyFinderResultHandler(exitValue);
       return resultHandler;	
	}
	/**
	 * 
	 * build command line parameters to extract dependency information to XML file
	 * Use Apache Commons Exec CommandLine to take care of forward/backward slashes and spaces in Windows and Linux environments
	 * e.g. C:\>DependencyExtractor -xml -out test.xml test.class
	 */
	CommandLine extract(Path classFilesRootFolder,Path outputFile){
		  Map<String,File> map = new HashMap<String,File>();
		  map.put("inputFile", classFilesRootFolder.toFile());
		  map.put("outputFile", outputFile.toFile());
    	  CommandLine commandline = new CommandLine(DEPENDENCYFINDER_HOME+File.separator+"bin"+File.separator+ "DependencyExtractor");
		  commandline.addArgument("-xml");
		  commandline.addArgument("-out");
		  commandline.addArgument("${outputFile}");
		  commandline.addArgument("${inputFile}");
		  commandline.setSubstitutionMap(map);
          return  commandline;
	}
	/**
	 * parse configuration property file 
	 */
	void getConfig(){
		Properties config = new Properties();
		try{
			config.load(Files.newInputStream(Paths.get(CONFIGFILE)));
			dependencyFinderCommand = config.getProperty("DEPENDENCYFINDER_COMMAND");
			outputFile = config.getProperty("OUTPUTFILE");
			jobTimeout = Integer.parseInt(config.getProperty("JOBTIMEOUT"));
			JAVA_HOME= config.getProperty("JAVA_HOME");
			DEPENDENCYFINDER_HOME = config.getProperty("DEPENDENCYFINDER_HOME");
			DEPENDENCYFINDER_OPTS = config.getProperty("DEPENDENCYFINDER_OPTS");
			
		}catch(IOException e){
			log.error("IOException in opening configuration file " + CONFIGFILE);
			System.exit(-1);
		}
	}
	
	Map setEnvironmentVariables(){
		@SuppressWarnings("rawtypes")
		Map env =null;
		try{
            env = EnvironmentUtils.getProcEnvironment();
            env.put("JAVA_HOME",JAVA_HOME);
            env.put("DEPENDENCYFINDER_HOME", DEPENDENCYFINDER_HOME);
            env.put("DEPENDENCYFINDER_OPTS", DEPENDENCYFINDER_OPTS);
		}catch(IOException e){
			e.printStackTrace();
			log.error("Error in updating environment variables");
		}
		return env;
	}


	
	 class DependencyFinderResultHandler extends DefaultExecuteResultHandler {

	        private ExecuteWatchdog watchdog;

	        public DependencyFinderResultHandler(ExecuteWatchdog watchdog)
	        {
	            this.watchdog = watchdog;
	        }

	        public DependencyFinderResultHandler(int exitValue) {
	            super.onProcessComplete(exitValue);
	        }
	        
	        public void onProcessComplete(int exitValue) {
	            super.onProcessComplete(exitValue);
	           log.debug("[resultHandler] The DependencyFinder was successfully executed ...");
	        }

	        public void onProcessFailed(ExecuteException e){
	            super.onProcessFailed(e);
	            if(watchdog != null && watchdog.killedProcess()) {
	                log.error("[resultHandler] The DependencyFinder process timed out");
	            }
	            else {
	                log.error("[resultHandler] The DependencyFinder process failed to do : " + e.getMessage());
	            }
	        }
	    }
}
