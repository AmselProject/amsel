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
import java.util.HashSet;
import java.util.Random;

/**
 * With PickAddress, addresses are either randomly chosen or tried sequentially from the given list of addresses.
 * 
 * Parameters expected in the configuration hash:
 * 
 * Integer numAddresses -- The number of addresses
 * String  address<i>   -- IP address <i>, where i=0, ..., numAddresses - 1
 * String  port<i>      -- IP port <i>, where i=0, ..., numAddresses -1
 * 
 * String  fallbackAddress -- The fallback address to be used if none of the given addresses resolves.
 * Integer fallbackPort    -- The fallback port to be used if none of the given addresses resolves.
 *  
 *  
 * Boolean random		-- If true, addresses will be tried randomly (with uniform distribution),
 *                         otherwise, they will be tried sequentially.
 * Boolean dryRun       -- If true, no host-name resolution will take place.
 * 
 * Integer maxAttempts  -- Number of lookup attempts before fallbackAddress is returned.
 * 
 * Optional parameters:
 * 
 * boolean allIP        -- If this is set to true, the module assumes that all addresses are given in dotted-decimal
 *                         notation and will not perform reverse lookup on them. If one of the addresses is not in
 *                         dotted-decimal format, something horrible might happen. The default value is false.
 * 
 *  
 * @author Philipp Reinecke, Hewlett Packard Labs Bristol, philipp.reinecke@hpe.com or philipp.reinecke@fu-berlin.de
 *
 */

public class PickAddress implements AddressProcess {

	final static String[] requiredParameters = new String[] {
			"numAddresses",
			"random",
			"maxAttempts",
			"fallbackAddress",
			"fallbackPort",
			"dryRun",
	};
	
	final static String[] optionalParameters = new String[] {
			"allIP",
			"requireDoRiskyStuff",
	};
	
	
	int numAddresses;   // The number of addresses given.
	int fallback;       // The index of the fallback address and fallback port in the addresses[] and ports[] arrays.
	String[] addresses; // Stores all of the addresses. The given addresses are at indices 0, ..., (numAddresses-1),
	                    // the fallback address is at index fallback (= numAddresses).  
	int[] ports;        // See comment for addresses[].
	
	boolean random;
	int maxAttempts;
	
	Random prng = new Random();
	
	boolean dryRun;
	
	boolean allIP = false;
	
	@Override
	public void configure(Hashtable<String, String> config) {
				
		if (Util.checkConfigRequired(this, config, requiredParameters)) {

			numAddresses = Integer.parseInt(config.get("numAddresses"));
			addresses = new String[numAddresses + 1];
			ports = new int[numAddresses + 1];
			
			for (int i = 0; i < numAddresses; i++) {
				if (config.containsKey("address" + i) && config.containsKey("port" + i)) {
					
					addresses[i] = config.get("address" + i);
					ports[i] = Integer.parseInt(config.get("port" + i));
						
				} else {
					System.err.println("UniformRandomPickAddress: Incomplete configuration.");
					System.exit(-1);
				}
			}

			fallback = numAddresses;
			addresses[fallback] = config.get("fallbackAddress"); 
			ports[fallback] = Integer.parseInt(config.get("fallbackPort"));
			
			random = Util.parseBoolean(config.get("random"));

			maxAttempts = Integer.parseInt(config.get("maxAttempts"));
			
			// Parse the optional parameters, if present.
			HashSet<String> optionalParametersPresent = Util.checkConfigOptional(this, config, optionalParameters);
			
			if (optionalParametersPresent.contains("noReverseLookup")) {
				allIP = Util.parseBoolean(config.get("noReverseLookup"));
			}
			
			
			dryRun = Util.parseBoolean(config.get("dryRun"));
			if (optionalParametersPresent.contains("requiredFlag")) {
				String requiredFlag = config.get("requiredFlag").trim();
				
				dryRun = dryRun | (! Util.cmdlineFlags.contains(requiredFlag));
			}
			
		} else {
			System.err.println("UniformRandomPickAddress: Incomplete configuration.");
			System.exit(-1);
		}
		
	}

	@Override
	public InternetAddress getSample() {
		
		if (random) {
			return randomAddress();
		} else {
			return sequentialAddress();
		}
		
	}
	
	
	/**
	 * Randomly pick an address from the list.
	 * 
	 * @return InternetAddress -- An IP address from the list. If the address was given as a hostname, the hostname has been resolved.
	 */
	private InternetAddress randomAddress() {
		
		InetAddress iaddr = null;
		
		for (int i = 0; i < maxAttempts; i++) {

			int j = prng.nextInt(numAddresses);

			iaddr = getAddress(j);
			
			// If iaddr is not null, then we have resolved a hostname and we can return its address.
			if (iaddr != null) {
				return new InternetAddress(iaddr, ports[j]);
			}
				
		}
		
		// If we get here, then we ran out of trials. We will return the fallback address.
		iaddr = getAddress(fallback, false);
		if (iaddr != null) {
			return new InternetAddress(iaddr, ports[fallback]);
		} else {
			System.err.println(this.getClass().getCanonicalName() + ".randomAddress(): Could not resolve fallback address. Exiting.");
			System.exit(-1);
		}

		// This statement should be unreachable.
		return null;
	}
	
	
	/**
	 * Sequentially try the addresses in the list and return the first one that can be resolved. 
	 * 
	 * @return InternetAddress -- An InternetAddress whose hostname could be resolved.
	 */
	private InternetAddress sequentialAddress() {
		int j = 0;
		
		InetAddress iaddr = null;
		for (int i = 0; i < maxAttempts; i++) {

			iaddr = getAddress(j);

			// If iaddr is not null, then we have resolved a hostname and we can return its address.
			if (iaddr != null) {
				return new InternetAddress(iaddr, ports[j]);
			}
			
			// Otherwise, we have to try again with the next hostname (rolling over if necessary).
			j = ++j >= numAddresses ? 0 : j;
			
		}

		// If we get here, then we ran out of trials. We will return the fallback address.
		iaddr = getAddress(fallback, false);
		if (iaddr != null) {
			return new InternetAddress(iaddr, ports[fallback]);
		} else {
			System.err.println(this.getClass().getCanonicalName() + ".sequentialAddress(): Could not resolve fallback address. Exiting.");
			System.exit(-1);
		}

		// This statement should be unreachable.
		return null;
	}

	
	/**
	 * Try to get an InetAddress for the address with the given index j, checking the settings of allIP and, optionally, dryRun.
	 * 
	 * @param j -- The index of the address in the addresses[] array.
	 * @param checkDryRun -- If set, the value of dryRun will be checked. If dryRun is set, then the routine will return null.
	 * @return null if both checkDryRun and dryRun are set or if the given address could not be resolved,
	 *         an InetAddress object containing the indicated address otherwise.	
	 */
	private InetAddress getAddress(int j, boolean checkDryRun) {
		if (checkDryRun && dryRun) {
			System.out.println("dryRun is set. Not looking up " + addresses[j]);
			return null;
		}

		try {
			if (! allIP) {
				return InetAddress.getByName(addresses[j]);
			} else {
				return InetAddress.getByAddress(Util.readOctets(addresses[j]));
			}
		} catch (UnknownHostException uhe) {
			return null;
		}
	}
	
	/**
	 * Try to get an InetAddress for the address with the given index j, checking the settings of allIP and dryRun.
	 * 
	 * @param j -- The index of the address in the addresses[] array.
	 * @return null if dryRun is set or the given address could not be resolved,
	 *         an InetAddress object containing the indicated address otherwise.
	 */
	private InetAddress getAddress(int j) {
		
		return getAddress(j, true);
		
	}

}
