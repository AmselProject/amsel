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

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.Hashtable;

/**
 * A StateBehaviour for injecting host symptoms.
 * 
 * @author Philipp Reinecke, Hewlett Packard Labs Bristol, philipp.reinecke@hpe.com or philipp.reinecke@fu-berlin.de
 *
 * Created: 7 Mar 2016 12:08:14
 *
 */

public class HostSymptomInjectorStateBehaviour implements StateBehaviour {

	final static String[] parameters = {
		"SymptomGenerator",
		"SymptomGeneratorConfig",
		"InterarrivalProcess",
		"InterarrivalProcessConfig",
	};
	
	
	HostSymptomGenerator generator;
	
	@Override
	public void configure(Hashtable<String, String> config) throws InstantiationException, IllegalAccessException, ClassNotFoundException {

		if (Util.checkConfigRequired(this, config, parameters)) {

			generator = (HostSymptomGenerator) Util.loadModule(config.get("SymptomGenerator"));
			generator.configure(Util.readConfigFile(config.get("SymptomGeneratorConfig")));
					
			InterarrivalProcess interarrivals = (InterarrivalProcess) Util.loadModule(config.get("InterarrivalProcess"));
			interarrivals.configure(Util.readConfigFile(config.get("InterarrivalProcessConfig")));
			
			generator.registerInterarrivalProcess(interarrivals);
						
		} else {
			System.exit(-1);
		}
		
	}

	@Override
	public void startBehaviour() {
		generator.startGenerator();
	}

	@Override
	public void stopBehaviour() {
		generator.stopGenerator();
	}

}
