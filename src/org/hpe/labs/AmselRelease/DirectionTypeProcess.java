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
 * This process returns the type of a data transfer as strings `IN' or `OUT'.
 * The probabilities of choosing either must be specified in the configuration
 * file.
 * 
 * Parameters expected in the configuration file:
 * 
 * Double (in [0, 1]) sendProbability -- Probability of returning `OUT'.
 * Double (in [0, 1]) recvProbability -- Probability of returning `IN'.
 * 
 * 
 * @author Philipp Reinecke, Hewlett Packard Labs Bristol, philipp.reinecke@hpe.com or philipp.reinecke@fu-berlin.de
 *
 */

public class DirectionTypeProcess implements TypeProcess<String> {

	double[] probabilities;
	
	Random prng = new Random();
	String[] directions = new String[] {"OUT", "IN"};
			
	
	@Override
	public void configure(Hashtable<String, String> config) {
		if (config.containsKey("sendProbability") &&
			config.containsKey("recvProbability")) {
			
			double sendProbability = Double.parseDouble(config.get("sendProbability"));
			double recvProbability = Double.parseDouble(config.get("recvProbability"));
			
			if (sendProbability < 0 ||
				recvProbability < 0 ||
				sendProbability + recvProbability != 1)
			{
				System.err.println("DirectionTypeProcess: Invalid probabilities");
				System.exit(-1);
			}
			
			probabilities = new double[] {sendProbability, recvProbability};
			
		} else {
			System.err.println("DirectionTypeProcess: Incomplete configuration.");
			System.exit(-1);
		}
	}

	@Override
	public String getSample() {
		
		return directions[Util.discreteRandom(prng, probabilities)];
		
	}

}
