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

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Hashtable;

/**
 * The main class for AMSEL. 
 * 
 * Parameters expected in the configuration file:
 * 
 * OperationalPhasesProcess       -- classname for the operational-phases process
 * OperationalPhasesProcessConfig -- configuration file for the operational-phases process
 * 
 * Integer runLength   -- Length of the run in seconds. Specify runLength < 0 to run forever. 
 * 
 * @author Philipp Reinecke, Hewlett Packard Labs Bristol, philipp.reinecke@hpe.com or philipp.reinecke@fu-berlin.de
 *
 */

public class AmselMain {

	private static final String[] requiredParameters = new String[] {
			"OperationalPhasesProcess",
			"OperationalPhasesProcessConfig",
			"runLength",
	};
	
	private static final String[] optionalParameters = new String[] {
			"configPathPrefix",
			"classPathPrefix",
			"globalDryRun",
			"cmdlineDryRun",
	};
	
	OperationalPhasesProcess opp;
	int runLength;
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		System.out.println("This is AMSEL version " + Util.VERSION);
		
		if (args.length < 1) {
			System.err.println("Please provide the name of exactly one configuration file.");
			System.exit(-1);
		}
		
		
		AmselMain am = new AmselMain();
		
		// Read configuration file.
		Hashtable<String, String> config = Util.readConfigFile(args[0]);
		
		// Check for optional flags that risky modules might require.
		if (args.length > 1) {
			String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
			Util.readCmdlineFlags(newArgs);
		}
			
		System.out.println("Command line flags: " + Util.cmdlineFlags);
		System.out.println("Configuration: " + config);
		
		am.configure(config);
		
		am.runAmselMain();
		
	}
	
	private void runAmselMain() {
		try {
			
			System.out.println("Running for " + runLength + " seconds.");
			
			opp.startBehaviour();
			
			if (runLength >= 0) {
				Thread.sleep(runLength * 1000);
			} else {
				// Sleep forever if runLength < 0
				Object obj = new Object();
			    synchronized (obj) {
			        obj.wait();
			    }
			}

			System.out.println("STOPPING.");

			Util.exitAllThreads = true;
			opp.stopBehaviour();
			System.out.println("STOPPED.");
		} catch (Exception e) {
			System.err.println("Exception caught: " + e);
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	private void configure(Hashtable<String, String> config) {
		
		if (! Util.checkConfigRequired(this, config, requiredParameters)) {
			System.err.println("Incomplete configuration file.");
			System.exit(-1);
		}

		try {
		
			if (config.containsKey("configPathPrefix")) {
				Util.configPathPrefix = config.get("configPathPrefix");
			}

			if (config.containsKey("classPathPrefix")) {
				Util.classPathPrefix = config.get("classPathPrefix");
			}

/*			if (config.containsKey("globalDryRun")) {
				Util.mainDryRunSetting = Util.parseBoolean(config.get("globalDryRun"));
			}
			
			if (config.containsKey("cmdlineDryRun")) {
				Util.cmdlineDryRunSetting = Util.parseBoolean(config.get("cmdlineDryRun"));
			}
			*/

			opp = (OperationalPhasesProcess) Util.loadModule(config.get("OperationalPhasesProcess"));
			opp.configure(Util.readConfigFile(config.get("OperationalPhasesProcessConfig")));
			
			runLength = Integer.parseInt(config.get("runLength"));
			
		} catch (Exception e) {
			System.err.println("Exception caught while configuring AmselMain: " + e);
			e.printStackTrace();
			System.exit(-1);
		}
		
	}
	
}
