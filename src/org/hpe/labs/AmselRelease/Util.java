/**
 * Copyright 2017 Hewlett Packard Enterprise Development LP.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * 
 */


package org.hpe.labs.AmselRelease;

import java.io.BufferedReader;
import java.io.FileReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.io.File;
import java.util.Hashtable;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;

import java.util.Random;

/**
 * Utility class containing all sorts of routines and variables needed
 * throughout. 
 * 
 * @author Philipp Reinecke, Hewlett Packard Labs Bristol, philipp.reinecke@hpe.com or philipp.reinecke@fu-berlin.de
 *
 */

public class Util {

	public static final String VERSION = "AmselRelease_2017-01-30";
	
	public static boolean exitAllThreads = false;
	
	public static String configPathPrefix = "";
	public static String classPathPrefix = "";
	
	private static DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	
	//public static boolean mainDryRunSetting = true;
	//public static boolean cmdlineDryRunSetting = true;
	
	public static HashSet<String> cmdlineFlags = new HashSet<String>();
	
	public static void readCmdlineFlags(String[] args) {
		cmdlineFlags.addAll(Arrays.asList(args));
	}
	
	public static Hashtable<String, String> readConfigFile(String filename) {
		Hashtable<String, String> ret = new Hashtable<String, String>();
		
		try {

			BufferedReader br = new BufferedReader(new FileReader(configPathPrefix + filename));
			String line;
			while ((line = br.readLine()) != null) {

				if (line.matches("^\\s*$") || line.matches("^#.*")) {
					continue;
				}
				
				String[] entry = line.split("\\s*=\\s*", 2);
				ret.put(entry[0], entry[1]);
				
			}
			
			br.close();

		} catch (Exception e) {
			System.err.println("Util.readConfigFile(`" + filename + "'):" + e);
			e.printStackTrace();
			
		}
		
		return ret;
		
	}
	
	public static File getFile(String name) {
		return new File(configPathPrefix + name);
	}
	
	public static int discreteRandom(Random prng, double[] probabilities) {
		int i = 0;
		double u = prng.nextDouble();
		double s = 0;
		
		while (s < u && i < probabilities.length - 1) {
			s += probabilities[i];
			if (s >= u) {
				break;
			}
			i++;
		}
		
		return i;
	}
	
	public static double exponentialRandom(Random prng, double intensity) {
		return intensity * Math.log(1 - prng.nextDouble());
	}
	
	// Check if the given configuration hash contains all required entries.
	// This could certainly be implemented in a more efficient manner.
	public static boolean checkConfigRequired(Object caller, Hashtable<String, String> config, String[] entries) {
		boolean complete = true;
		
		for (String e : entries) {
			if (! config.containsKey(e)) {
				complete = false;
				System.err.println("Missing entry `" + e + "' while configuring " + caller.getClass().getCanonicalName());
			}
		}
		
		return complete;
	}
	
	/**
	 * Check if the given configuration hash contains all the given entries and return a HashSet<String> containing the entries that are present. 
	 * 
	 * @param callerName
	 * @param config
	 * @param entries
	 * @return
	 */
	public static HashSet<String> checkConfigOptional(Object caller, Hashtable<String, String> config, String[] entries) {
		
		HashSet<String> present = new HashSet<String>();

		for (String e : entries) {
			if (config.containsKey(e)) {
				present.add(e);
			} else {
				System.err.println("Missing optional entry `" + e + "' while configuring " + caller.getClass().getCanonicalName() + ". The default value will be used.");
			}
		}

		return present;
	}

	public static String[] parseStringArray(String s) {
		return s.split("\\s*,\\s*");
	}
	
	public static int[] parseIntArray(String s) {
		String[] a = parseStringArray(s);
		int[] ret = new int[a.length];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = Integer.parseInt(a[i]);
		}
		
		return ret;
	}
	
	public static double[] parseDoubleArray(String s) {
		String[] a = parseStringArray(s);
		double[] ret = new double[a.length];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = Double.parseDouble(a[i]);
		}
		
		return ret;
	}
	
	public static Object loadModule(String name) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		return Class.forName(classPathPrefix + name).newInstance();
	}
	
	/**
	 * Split the given IPv4 address into octets. 
	 * 
	 * @param s An IPv4 address in octet form: abc.def.ghi.jkl.
	 * @return A byte array containing the octets, suitable for passing to InetAddress.getByAddress().
	 */
	public static byte[] readOctets(String s) {
		String[] octets = s.split("\\.");
		byte[] ret = new byte[octets.length];
		for (int i=octets.length-1; i >= 0; i--) {
			ret[i] = (byte) Integer.parseInt(octets[i]);
		}
		
		return ret;
	}
	
	/**
	 * Parse the given String as boolean, making sure that all whitespace is removed.
	 * 
	 * @param s
	 * @return
	 */
	public static boolean parseBoolean(String s) {
		return Boolean.parseBoolean(s.trim());
	}
	
	
	public static void logTime(String s) {
		System.out.println(dateFormat.format(new Date()) + " " + s);
	}

//	/**
//	 * Parse the given dryRun parameter. criticality indicates how critical the dryRun parameter
//	 * is for the caller. The higher the value, the more checks are placed on the value:
//	 * 
//	 *  0 : The return value only depends on the given parameter.
//	 *  1 : Both the the parameter and the setting in the configuration file must be true for the return value to be true.
//	 *  2 : The parameter, the setting in the configuration file, and the setting on the command line must be true for the
//	 *  	return value to be true.
//	 *  >2: false.
//	 * 
//	 * @param s
//	 * @return
//	 */
//	public static boolean parseDryRun(String s, int criticality) {
//		switch (criticality) {
//		case 0:
//			return parseBoolean(s);
//		case 1:
//			return parseBoolean(s) | mainDryRunSetting;
//		case 2: 
//			return parseBoolean(s) | mainDryRunSetting | cmdlineDryRunSetting;
//		
//		default:
//			return false;
//		}
//	}
}
