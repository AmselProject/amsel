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
 * The NetworkSymptomInjectorStateBehaviour describes a state in which network symptoms are
 * injected.
 * 
 * The following parameters are expected in the configuration file:
 * 
 * TrafficGenerator
 * 
 * InterarrivalProcess
 * InterarrivalProcessConfig
 * 
 * PacketSizeProcess
 * PacketSizeProcessConfig
 * 
 * AddressProcess
 * AddressProcessConfig
 * 
 * Optional:
 * 
 * TypeProcess
 * TypeProcessConfig
 * 
 * @author Philipp Reinecke, Hewlett Packard Labs Bristol, philipp.reinecke@hpe.com or philipp.reinecke@fu-berlin.de
 *
 */

public class NetworkSymptomInjectorStateBehaviour implements StateBehaviour {

	TrafficGenerator trafficGenerator;
	
	@Override
	public void configure(Hashtable<String, String> config) throws InstantiationException, IllegalAccessException, ClassNotFoundException {

		trafficGenerator = (TrafficGenerator) Util.loadModule(config.get("TrafficGenerator"));			
		InterarrivalProcess arrivals = (InterarrivalProcess) Util.loadModule(config.get("InterarrivalProcess"));
		PacketSizeProcess packetSizes = (PacketSizeProcess) Util.loadModule(config.get("PacketSizeProcess"));
		AddressProcess addresses = (AddressProcess) Util.loadModule(config.get("AddressProcess"));
		TypeProcess types = null;
		if (config.containsKey("TypeProcess")) {
			types = (TypeProcess) Util.loadModule(config.get("TypeProcess"));
		}
	
		arrivals.configure(Util.readConfigFile(config.get("InterarrivalProcessConfig")));
		packetSizes.configure(Util.readConfigFile(config.get("PacketSizeProcessConfig")));
		addresses.configure(Util.readConfigFile(config.get("AddressProcessConfig")));
		if (types != null) {
			types.configure(Util.readConfigFile(config.get("TypeProcessConfig")));
		}

		trafficGenerator.registerInterarrivalProcess(arrivals);
		trafficGenerator.registerPacketSizeProcess(packetSizes);
		trafficGenerator.registerAddressProcess(addresses);
		trafficGenerator.registerTypeProcess(types);
		
		trafficGenerator.configure(Util.readConfigFile(config.get("TrafficGeneratorConfig")));
	}

	@Override
	public void startBehaviour() {
		// TODO Auto-generated method stub
		trafficGenerator.startTraffic();
	}

	@Override
	public void stopBehaviour() {
		// TODO Auto-generated method stub
		trafficGenerator.stopTraffic();
	}

}
