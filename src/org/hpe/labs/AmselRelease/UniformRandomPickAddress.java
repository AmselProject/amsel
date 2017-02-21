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
import java.util.Hashtable;
import java.util.Random;

/**
 * This module has been replaced by PickAddress. Unless you really need the old behaviour, please only use the new one.
 * 
 * With UniformRandomPickAddress, addresses are randomly chosen (with
 * uniform distribution) from the given list of addresses.
 * 
 * Parameters expected in the configuration hash:
 * 
 * Integer numAddresses -- The number of addresses
 * String  address<i>   -- IP address <i>, where i=0, ..., numAddresses - 1
 * String  port<i>      -- IP port <i>, where i=0, ..., numAddresses -1
 *  
 * @author Philipp Reinecke, Hewlett Packard Labs Bristol, philipp.reinecke@hpe.com or philipp.reinecke@fu-berlin.de
 *
 */

public class UniformRandomPickAddress implements AddressProcess {

	InternetAddress[] addresses;
	Random prng = new Random();
	
	@Override
	public void configure(Hashtable<String, String> config) {
				
		if (config.containsKey("numAddresses")) {

			int num = Integer.parseInt(config.get("numAddresses"));
			addresses = new InternetAddress[num];
			
			for (int i=0; i<num; i++) {
				if (config.containsKey("address" + i) && config.containsKey("port" + i)) {
					
					try {
						String address = config.get("address" + i);
						InetAddress iaddress = InetAddress.getByName(address);
					
						int port = Integer.parseInt(config.get("port" + i));
						
						addresses[i] = new InternetAddress(iaddress, port);
						
					} catch (Exception uhe) {
						System.err.println("UniformRandomPickAddress: " + uhe);
						System.exit(-1);
					}
					
				} else {
					System.err.println("UniformRandomPickAddress: Incomplete configuration.");
					System.exit(-1);
				}
			}
			
		} else {
			System.err.println("UniformRandomPickAddress: Incomplete configuration.");
			System.exit(-1);
		}
	}

	@Override
	public InternetAddress getSample() {
		
		return addresses[prng.nextInt(addresses.length)];
	
	}

}
