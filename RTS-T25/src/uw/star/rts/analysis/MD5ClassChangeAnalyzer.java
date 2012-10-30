package uw.star.rts.analysis;
import java.nio.file.*;
import uw.star.rts.artifact.*;
import java.util.*;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.*;
import java.io.*;
/**
 * This class compute differences between versions of the program to identify changed classes. The class is implemented by comparing
 * MD5 digest(hash) of the two class files. This class uses Apache Commons Codec library
 * @see http://commons.apache.org/codec/api-release/org/apache/commons/codec/digest/DigestUtils.html 
 * @see http://commons.apache.org/codec/
 *  
 * @author Weining Liu
 *
 */
public  class MD5ClassChangeAnalyzer {
	private static Logger log;
	public static String NEW_CLASSENTITY_KEY = "NEW";
	public static String MODIFIED_CLASSENTITY_KEY = "MODIFIED";
	public static String DELETED_CLASSENTITY_KEY = "DELETED";
	
	public MD5ClassChangeAnalyzer(){
		log = LoggerFactory.getLogger(MD5ClassChangeAnalyzer.class.getName());
	}
	/**
	 * Compare a program v0 to another program v1 and return new classes in v1 , modified classes in v0, and deleted classes in v0
	 *  
	 * @param v0
	 * @param v1
	 * @return a map with key "NEW" "MODIFIED" "DELETED"
	 */
	public static Map<String,List<ClassEntity>> diff(Program v0,Program v1){
		Map<String,List<ClassEntity>> resultMap = new HashMap<String,List<ClassEntity>>();
		List<ClassEntity> newClassEntities = new ArrayList<ClassEntity>();
		List<ClassEntity> modifiedClassEntities = new ArrayList<ClassEntity>();
		List<ClassEntity> deletedClassEntities = new ArrayList<ClassEntity>();
		List <ClassEntity> v1ClassEntities = (List <ClassEntity>)v1.getCodeEntities(EntityType.CLAZZ);
		BitSet v1OldEntites = new BitSet(v1ClassEntities.size());// this is used to track whether an entity in v1 is new comparing to v0. every bit has initial value of false, which means entity is new
	
		for(Entity e0: v0.getCodeEntities(EntityType.CLAZZ)){
			ClassEntity c0 = (ClassEntity)e0;
			boolean c0IsFoundinV1 = false;
			for(int i=0;i<v1ClassEntities.size();i++){
				 ClassEntity c1 = (ClassEntity)v1ClassEntities.get(i);
				if(c0.getPackageName().equals(c1.getPackageName())&&c0.getClassName().equals(c1.getClassName())){
				//if c0 is found in v1, then c1 is not new
					c0IsFoundinV1 = true;
					v1OldEntites.set(i); // set to true , which means this entity is old
				//compare MD5 digest of c0,c1, add c0 to modified list if different
					if(!hasSameMD5Digest(c0.getArtifactFile(), c1.getArtifactFile()))
						modifiedClassEntities.add(c0);
					break; // don't need to go through the rest of v1
				}
			}
			//finished iterating the complete v1 but c0 is still not found , add c0 to the deleted list
			if(!c0IsFoundinV1)  deletedClassEntities.add(c0);
		}
		
		//add all new entities in v1 to newClassEntities, every bit that has false value
		if(v1ClassEntities.size()>0){  //v1 is not empty
			int nextIdx = v1OldEntites.nextClearBit(0);
		   while(nextIdx<v1ClassEntities.size()){
			   newClassEntities.add((ClassEntity)v1ClassEntities.get(nextIdx));
			   nextIdx = v1OldEntites.nextClearBit(nextIdx+1);
		   }
		}
		resultMap.put(NEW_CLASSENTITY_KEY,newClassEntities);
		resultMap.put(MODIFIED_CLASSENTITY_KEY, modifiedClassEntities);
		resultMap.put(DELETED_CLASSENTITY_KEY, deletedClassEntities);
		return resultMap;
	}
	

	/**
	 * compare whether 2 files have different MD5 hash
	 * @param f0
	 * @param f1
	 * @return true if files have the same MD5 digest, false otherwise. If any of the files can not be opened, return false
	 */
	public static boolean hasSameMD5Digest(Path f0, Path f1){
		try{
			//System.out.println("Calculate md5hex of " + f0);
			String f0md5 = DigestUtils.md5Hex(Files.newInputStream(f0));
			
			String f1md5 = DigestUtils.md5Hex(Files.newInputStream(f1));
			return f0md5.equals(f1md5);
		}catch(IOException e){
			log.error("Error opening file " + f0 + " or " + f1);
		}
		return false;
	}
}
