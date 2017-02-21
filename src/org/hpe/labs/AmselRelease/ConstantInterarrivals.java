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

/**
 * The ConstantInterarrivals process always returns the same interarrival time.
 * 
 * Parameters:
 * 
 * Double interarrivalTime -- interarrival time in seconds
 * 
 * @author Philipp Reinecke, Hewlett Packard Labs Bristol, philipp.reinecke@hpe.com or philipp.reinecke@fu-berlin.de
 *
 * Created: 1 Feb 2016 12:03:29
 *
 */

public class ConstantInterarrivals implements InterarrivalProcess {

	final static String[] requiredParameters = new String[] {
			"interarrivalTime",
	};
	
	final static String[] optionalParameters = new String[] {
	};

	double interarrivalTime;
	
	@Override
	public void configure(Hashtable<String, String> config) {
		if (Util.checkConfigRequired(this, config, requiredParameters))
		{
			interarrivalTime = Double.parseDouble(config.get("interarrivalTime")) * 1000;
			
		} else {
			System.exit(-1);
		}
	}

	@Override
	public Double getSample() {
		return interarrivalTime;
	}

}
