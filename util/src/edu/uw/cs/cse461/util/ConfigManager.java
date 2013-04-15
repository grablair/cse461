package edu.uw.cs.cse461.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * A class intended to make parsing the config.ini file easier.  
 * See ConfigManagerInterface for further documentation and a concise list of public methods.
 * @author zahorjan
 *
 */
public class ConfigManager extends Properties implements ConfigManagerInterface {
	private static final String TAG = "ConfigManager";
	
	/**
	 * Can't pass in the more natural config file name because Android won't tell us
	 * where it is (won't tell us the path to our assets).  Instead, caller must
	 * open the file.
	 * @param configFileInputStream  Attached to the config file.
	 * @throws IOException
	 */
	public ConfigManager(FileInputStream configFileInputStream) throws IOException {
		load(configFileInputStream);
		// we avoid some confusion by getting rid of trailing whitespace on values.
		// there doesn't seem to be a way to both delete the old key and insert the new one while iterating!
		ArrayList<String> badKeys = new ArrayList<String>(); 
		Iterator it = entrySet().iterator();
		while ( it.hasNext() ) {
			Map.Entry entry = (Map.Entry)it.next();
			String key = (String)entry.getKey();
			String trimmedkey = key.trim();
			if ( !key.equals(trimmedkey)) {
				badKeys.add(key);
				continue;
			}
			String value = getProperty(key);
			String trimmed = value.trim();
			if ( !value.equals(trimmed) ) {
				badKeys.add(key);
			}
		}
		for ( String key : badKeys ) {
			String value = getProperty(key);
			remove(key);
			
			String trimmedKey = key.trim();
			String trimmedValue = value.trim();
			setProperty(trimmedKey, trimmedValue);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	//
	// Methods that read or verify correctness of whitespace separated vectors in the config file.
	// (For instance, fields net.services, ddns.service.nodes, and ddns.service.init.)
	// 
	//-------------------------------------------------------------------------------------------------
	
	
	/**
	 * Reads entries like test.uw12au.cse461.:IP:port
	 * @param fieldName
	 * @return
	 * @throws RuntimeException
	 */
	public ArrayList<String[]> readNameIPPortVec(String fieldName) throws RuntimeException {
		ArrayList<String[]> result = readParsedVector(fieldName);
		// each element should be a vector of length 3, except cname is length 4
		for ( String[] element : result ) {
			if ( element.length != 3 ) throw new RuntimeException("Illegal config entry for field " + fieldName + ".  Should be ddnsname:ip:port");
		}
		return result;
//		return readNameRecordVec(fieldName);
	}	
	
	/**
	 * Reads entries like jz.cse461.:password
	 * @param fieldName
	 * @return Null if the field doesn't appear in the config file; an empty vector if it appears but is defined to be null; otherwise, the vector of String tuples 
	 */
	public ArrayList<String[]> readNamePasswordVec(String fieldName) throws RuntimeException {
		ArrayList<String[]> result = readParsedVector(fieldName);
		if ( result == null ) return null;
		// each element should be a vector of length 2
		for ( String[] element : result ) {
			if ( element.length != 2 ) throw new RuntimeException("Error in config file entry " + fieldName + ".  Each entry should have 2 fields.");
		}
		return result;
	}
	
	/**
	 * Reads entries like a:jz.cse461.:password and cname:jz.cse461:foo.cse461:password
	 * @param fieldName
	 * @return
	 * @throws RuntimeException
	 */
	public ArrayList<String[]> readNameRecordVec(String fieldName) throws RuntimeException {
		ArrayList<String[]> result = readParsedVector(fieldName);
		// each element should be a vector of length 3, except cname is length 4
		for ( String[] element : result ) {
			if ( element.length != 3 &&
				 (element.length != 4 || !element[0].equals("cname"))
			   ) throw new RuntimeException("Illegal config entry for field " + fieldName );
		}
		return result;
	}
	
	/**
	 * Takes a space separated list of colon separated tokens and returns an ArrayList of String arrays.
	 * @param fieldName
	 * @return
	 * @throws RuntimeException
	 */
	private ArrayList<String[]> readParsedVector(String fieldName) throws RuntimeException {
		String[] strVec = getAsStringVec(fieldName);
		if ( strVec == null ) return null;
		ArrayList<String[]> result = new ArrayList<String[]>();
		for (String entry : strVec ) {
			result.add(entry.split("[:]"));
		}
		return result;
	}	

	/**
	 * Reads a single config file field whose value is a white space separated list of tokens.
	 * (E.g., net.services.)
	 * @return An array of tokens if  the field exists and has a vaule; otherwise null.
	 */
	@Override
	public String[] getAsStringVec(String key) {
		String value = getProperty(key);
		if ( value == null ) return null;
		return value.split("[\\s]+");
	}
	
	//-------------------------------------------------------------------------------------------------
	//
	// Methods that read or scalars from the config file.
	// (Note: the Properties superclass provides additional methods.)
	// 
	//-------------------------------------------------------------------------------------------------
	
	/**
	 * Returns a field value from the config file as an integer.  Throws an exception if it doesn't exist
	 * or doesn't represent an integer.
	 */
	@Override
	public int getAsInt(String key) throws NoSuchFieldException {
		String valStr = getProperty(key);
		if ( valStr == null ) throw new NoSuchFieldException("No " + key + " entry found in config file");
		return Integer.parseInt(valStr);
	}
	
	/**
	 * Returns a field value from the config file as an integer.  Returns the default value argument
	 * if the field doesn't exist.
	 */
	@Override
	public int getAsInt(String key, int defaultVal) {
		return getAsInt(key, defaultVal, Integer.MIN_VALUE, Integer.MAX_VALUE);
	}
	
	/**
	 * Returns a field value from the config file as an integer.  Returns the default value
	 * if the config file field value cannot be converted to an integer.  Returns the minimum
	 * value if the value that otherwise would be returned is less than the minimum.
	 */
	@Override
	public int getAsInt(String key, int defaultVal, int minimum) {
		return getAsInt(key, defaultVal, minimum, Integer.MAX_VALUE);
	}
	
	/**
	 * Returns a field value from the config file as an integer.  Returns the default value
	 * if the config file field value cannot be converted to an integer.  Returns the minimum
	 * value if the value that otherwise would be returned is less than the minimum.  Returns
	 * the maximum value if the value that otherwise would be returned is greater than the maximum.
	 */
	@Override
	public int getAsInt(String key, int defaultVal, int minimum, int maximum) {
		int result;
		try {
			result = getAsInt(key);
		} catch (Exception e) {
			Log.i(TAG, "Missing or non-integer value for config entry " + key + ".  Using default " + defaultVal);
			result = defaultVal;
		}
		
		if ( result < minimum ) {
			Log.w(TAG, key + " value " + result + " is below minimum (" + minimum + ").  Resetting to minimum.");
			result = minimum;
		}
		else if ( result > maximum ) {
			Log.w(TAG, key + " value " + result + " is above (" + maximum + ").  Resetting to maximum.");
			result = maximum;
		}
		return result;
	}
}
