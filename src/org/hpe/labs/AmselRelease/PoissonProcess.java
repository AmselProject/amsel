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
 * The humble Poisson process. Interarrival times follow an exponential
 * distribution with given rate.
 * 
 * Parameters expected in the configuration hash:
 * 
 * Double rate -- the rate of the interarrival-time distribution.
 * 
 * @author Philipp Reinecke, Hewlett Packard Labs Bristol, philipp.reinecke@hpe.com or philipp.reinecke@fu-berlin.de
 *
 */

public class PoissonProcess implements InterarrivalProcess {

	double intensity;
	Random prng = new Random();
	
	public void configure(Hashtable<String, String> config) {
		
		if (config.containsKey("rate")) {

			this.setRate(Double.parseDouble(config.get("rate")));
			
		} else {
			System.err.println("PoissonProcess: No `rate' parameter in configuration hashtable.");
			System.exit(-1);
		}
		
	}
	
	public void setRate(double rate) {
		intensity = 1000 * (-1/rate);
	}
	
	@Override
	public Double getSample() {
		return intensity * Math.log(1 - prng.nextDouble());
	}

}
