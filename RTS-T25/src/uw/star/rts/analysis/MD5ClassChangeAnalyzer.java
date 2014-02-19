package uw.star.rts.analysis;
import java.nio.file.*;

import uw.star.rts.artifact.*;
import uw.star.rts.util.ClassEntityComparator;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.*;

import com.google.common.hash.*;
import com.google.common.io.ByteStreams;

import java.io.*;
/**
 * This class compute differences between versions of the program to identify changed classes. The class is implemented by comparing
 *  
 * @author Weining Liu
 *
 */
public  class MD5ClassChangeAnalyzer {
	private static Logger log = LoggerFactory.getLogger(MD5ClassChangeAnalyzer.class.getName());
	public static String NEW_CLASSENTITY_KEY = "NEW";
	public static String MODIFIED_CLASSENTITY_KEY = "MODIFIED";
	public static String DELETED_CLASSENTITY_KEY = "DELETED";
	public static void main(String[] args){
		Path p0 = Paths.get(args[0]);
		Path p1 = Paths.get(args[1]);
		System.out.println("googelHashing - " + googelHashing(p0,p1));
		System.out.println("javaMessageDigest - " + javaMessageDigest(p0,p1));
		System.out.println("apacheCommons - " + apacheCommons(p0,p1));
	}

	/**
	 * Compare a program v0 to another program v1 and return new classes in v1 , modified classes in v0, and deleted classes in v0
	 * {DELETED} = {v0} - {v1}
	 * {NEW} = {V1}-{V0}
	 * {MODIFIED} = {V0} INTERSECT {V1} and e0 and e1 does not have the same MD5digest
	 *  
	 * @param v0
	 * @param v1
	 * @return a map with key "NEW" "MODIFIED" "DELETED"
	 */
	public static Map<String,List<ClassEntity>> diff(Program v0,Program v1){
	  ClassEntityComparator comparator = new ClassEntityComparator();
		
		List<ClassEntity> newClassEntities = new ArrayList<ClassEntity>();
		List<ClassEntity> modifiedClassEntities = new ArrayList<ClassEntity>();
		List<ClassEntity> deletedClassEntities = new ArrayList<ClassEntity>();
		List <ClassEntity> v1ClassEntities = (List <ClassEntity>)v1.getCodeEntities(EntityType.CLAZZ);
		BitSet v1OldEntites = new BitSet(v1ClassEntities.size());// this is used to track whether an entity in v1 is new comparing to v0. every bit has initial value of false, which means entity is new

		for(Entity e0: v0.getCodeEntities(EntityType.CLAZZ)){
			boolean c0IsFoundinV1 = false;
			for(int i=0;i<v1ClassEntities.size();i++){
				if(comparator.compare((ClassEntity)e0,(ClassEntity)(v1ClassEntities.get(i)))==0){
					//if c0 is found in v1, then c1 is not new
					c0IsFoundinV1 = true;
					v1OldEntites.set(i); // set to true , which means this entity is old
					//compare MD5 digest of c0,c1, add c0 to modified list if different
					if(!hasSameMD5Digest(((ClassEntity)e0).getArtifactFile(), ((ClassEntity)v1ClassEntities.get(i)).getArtifactFile()))
						modifiedClassEntities.add((ClassEntity)e0);
					break; // don't need to go through the rest of v1
				}
			}
			//finished iterating the complete v1 but c0 is still not found , add c0 to the deleted list
			if(!c0IsFoundinV1)  deletedClassEntities.add((ClassEntity)e0);
		}

		//add all new entities in v1 to newClassEntities, every bit that has false value
		if(v1ClassEntities.size()>0){  //v1 is not empty
			int nextIdx = v1OldEntites.nextClearBit(0);
			while(nextIdx<v1ClassEntities.size()){
				newClassEntities.add((ClassEntity)v1ClassEntities.get(nextIdx));
				nextIdx = v1OldEntites.nextClearBit(nextIdx+1);
			}
		}
		Map<String,List<ClassEntity>> resultMap = new HashMap<String,List<ClassEntity>>();
		resultMap.put(NEW_CLASSENTITY_KEY,newClassEntities);
		resultMap.put(MODIFIED_CLASSENTITY_KEY, modifiedClassEntities);
		resultMap.put(DELETED_CLASSENTITY_KEY, deletedClassEntities);
		return resultMap;
	}


	/**
	 * compare whether 2 files have different MD5 hash.
	 * use Google Hashing function as the primary result, but also compare with other methods to be sure.
	 * @param f0
	 * @param f1
	 * @return true if files have the same MD5 digest, false otherwise. If any of the files can not be opened, return false
	 */
	public static boolean hasSameMD5Digest(Path f0, Path f1){
		if(!(Files.exists(f0)&&Files.exists(f1))){
			log.error("Either " + f0 + " or " + f1 + " does not exist");
			throw new IllegalArgumentException("Either " + f0.getFileName() + " or " + f1.getFileName() + " does not exist");
		}
		
		boolean result = googelHashing(f0,f1);
		if(result!=javaMessageDigest(f0,f1)||result!=apacheCommons(f0,f1))
			log.error("MD5 hashing error - hashing results are different for " + f0 + " and " + f1);
		return result;
	}

	private static boolean googelHashing(Path f0,Path f1){
		try(InputStream s0 =Files.newInputStream(f0);
				InputStream s1 =Files.newInputStream(f1);){
			HashFunction hf = Hashing.md5(); //this can be reused
			HashCode hc0 = hf.newHasher().putBytes(ByteStreams.toByteArray(s0)).hash();
			HashCode hc1 = hf.newHasher().putBytes(ByteStreams.toByteArray(s1)).hash();
			return hc0.equals(hc1);
		}catch(IOException e){
			e.printStackTrace();
		}
		return false;
	}

	private static boolean javaMessageDigest(Path f0,Path f1) {

		try(InputStream s0 =Files.newInputStream(f0);
				InputStream s1 =Files.newInputStream(f1);){
			MessageDigest md1 = MessageDigest.getInstance("MD5");
			String ss0 = Hex.encodeHexString(md1.digest(ByteStreams.toByteArray(s0)));
			MessageDigest md2 = MessageDigest.getInstance("MD5");
			String ss1 = Hex.encodeHexString(md2.digest(ByteStreams.toByteArray(s1)));
			return ss0.equals(ss1);
		}catch(IOException e){
			e.printStackTrace();
		}catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	private static boolean apacheCommons(Path f0,Path f1){
		try(InputStream s0 =Files.newInputStream(f0);
				InputStream s1 =Files.newInputStream(f1);){
			return DigestUtils.md5Hex(s0).equals(DigestUtils.md5Hex(s1));
		}
		catch(IOException e){
			e.printStackTrace();
		}
		return false;
	}
}
