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
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Random;

/**
 * Generate domain names using a simple Domain-Generation Algorithm (DGA),
 * and return their addresses.
 * 
 * Parameters expected in the configuration hash:
 * 
 * String  tlds         -- White-space-separated list of TLDs to use.
 * Integer port         -- IP port
 * Integer length       -- Length of the generated name (excluding TLD)
 * Integer maxAttempts  -- Maximum number of hosts tried
 * String  characters   -- Set of characters to choose from
 * String  fallback     -- Fallback address which will be chosen if no hostname can be resolved
 * 
 * 
 * Boolean dryRun       -- Unless this is false, no host-name resolution will take place.
 *
 * Optional:
 * 
 * Integer cacheTime    -- Length of time to remember the previously returned address for, in seconds.
 * requireDoRiskyStuff  -- If set, both dryRun=true and the command line doRiskyStuff must be given, otherwise the module
 *                      -- will operate in dryRun mode.
 * 
 * @author Philipp Reinecke, Hewlett Packard Labs Bristol, philipp.reinecke@hpe.com or philipp.reinecke@fu-berlin.de
 *
 */

public class Dga1Address implements AddressProcess {

	Random prng = new Random();

	String[] tlds;
	int port;
	int length;
	int maxAttempts;
	String characters;
	InternetAddress fallback;
	boolean dryRun;
	
	InternetAddress previousSample;
	int cacheTime = -1;
	long previousSampleTime = 0;
	
	@Override
	public void configure(Hashtable<String, String> config) {

		if (Util.checkConfigRequired(this, config, new String[] {"suffixes", "length", "maxAttempts", "characters", "fallbackAddress", "dryRun"})) {
			try {
				tlds        = config.get("suffixes").split("\\s+");
				characters  = config.get("characters");
				port        = Integer.parseInt(config.get("port"));
				length      = Integer.parseInt(config.get("length"));
				maxAttempts = Integer.parseInt(config.get("maxAttempts"));

				String fb   = config.get("fallbackAddress");
				fallback    = new InternetAddress(InetAddress.getByName(fb), port);
				
				dryRun      = Util.parseBoolean(config.get("dryRun"));
				if (config.containsKey("requiredFlag")) {
					dryRun = dryRun | (! Util.cmdlineFlags.contains(config.get("requiredFlag")));
				}
				
				if (config.containsKey("cacheTime")) {
					this.cacheTime = Integer.parseInt(config.get("cacheTime")) * 1000;
				}

			} catch (Exception uhe) {
				System.err.println("Dga1Address: " + uhe);
				System.exit(-1);
			}
		} else {
			System.exit(-1);
		}
					
	}

	@Override
	public InternetAddress getSample() {

		long time = System.currentTimeMillis() / 1000;
		
		if (System.currentTimeMillis() < previousSampleTime + cacheTime) {
			System.out.println("Within cache time, returning " + previousSample);
			return previousSample;
		}
		
		InternetAddress ret = null;
		
		Random nprng = new Random(time);

		int trial = 0;
		
		while (trial < maxAttempts) {
			
			String name = "";
			
			for (int i=0; i < length; i++) {
				name = name + characters.charAt(nprng.nextInt(characters.length()));
			}

			name = name + tlds[nprng.nextInt(tlds.length)];
			System.out.println("Dga1Address: " + name);
			try {
				if (! dryRun) {
					InternetAddress address = new InternetAddress(InetAddress.getByName(name), port);
					ret = address;
					break;
				} else {
					System.out.println("dryRun is active. Not looking up address.");
				}
			} catch (UnknownHostException uhe) {
			}
			trial++;
		}

		if (ret == null) {
			ret = fallback;
		}

		previousSampleTime = System.currentTimeMillis();
		previousSample = ret;
		
		return ret;
		
	}

}
