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


import java.util.Hashtable;
import java.util.Random;

/**
 * With a UniformRandomPacketSize process, packet sizes follow a
 * Uniform(a, b) distribution.
 * 
 * Parameters expected in the configuration hash:
 * 
 * Integer lowerLimit -- lower limit of the range (inclusive)
 * Integer upperLimit -- upper limit of the range (inclusive)
 * 
 * @author Philipp Reinecke, Hewlett Packard Labs Bristol, philipp.reinecke@hpe.com or philipp.reinecke@fu-berlin.de
 *
 */

public class UniformRandomPacketSize implements PacketSizeProcess {

	int a;
	int b;
	int limit;
	
	Random prng = new Random();
	
	@Override
	public void configure(Hashtable<String, String> config) {

		if (config.containsKey("lowerLimit") && config.containsKey("upperLimit")) {

			this.setLimits(Integer.parseInt(config.get("lowerLimit")), 
					Integer.parseInt(config.get("upperLimit"))); 
			
		} else {
			System.err.println("UniformRandomPacketSize: Incomplete configuration.");
			System.exit(-1);
		}
		
	}
	
	public void setLimits(int a, int b) {
		
		if (a > b) {
			System.err.println("UniformRandomPacketSize: a > b");
			System.exit(-1);
		}
		
		this.a = a;
		this.b = b;
		this.limit = (b-a) + 1;
		
		//System.out.println("UniformRandomPacketSize: " + a + ", " + b);
		
	}

	@Override
	public Integer getSample() {
		int ret = a + prng.nextInt(limit);
		
		//System.out.println("Returning " + ret + " [" + a + ", " + b + "]");

		return ret;
	}
	
	@Override
	public String toString() {
		return "UniformRandomPacketSize [" + a + ", " + b + "]";
	}

}
