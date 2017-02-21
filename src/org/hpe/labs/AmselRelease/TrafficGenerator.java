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

/**
 * A TrafficGenerator generates traffic according to the processes
 * registered with it.
 *
 * Every implementation should expect that startTraffic() is used to
 * 
 * (a) initially start traffic generation (i.e. no traffic should be
 *     generated before startTraffic() has been called),
 * (b) resume traffic generation after it has been paused with stopTraffic().
 * 
 * Every implementation also must expect the use of stopTraffic() for
 * 
 * (a) pausing traffic generation (i.e. traffic generation should cease after
 *     stopTraffic() has been called, but the process must be resumable by
 *     startTraffic()),
 * (b) permanently stopping traffic generation (which will be signalled by
 *     Util.exitAllThreads == true).
 * 
 * @author Philipp Reinecke, Hewlett Packard Labs Bristol, philipp.reinecke@hpe.com or philipp.reinecke@fu-berlin.de
 *
 */

import java.util.Hashtable;

public interface TrafficGenerator {

	public void registerInterarrivalProcess(InterarrivalProcess p);
	public void registerPacketSizeProcess(PacketSizeProcess p);
	public void registerAddressProcess(AddressProcess p);
	public void registerTypeProcess(TypeProcess p);
	
	public void configure(Hashtable<String, String> config);
	
	public void startTraffic();
	public void stopTraffic();
	
}
