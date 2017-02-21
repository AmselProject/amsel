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
 * A Markov-Modulated Interarrival Process. This process is governed by an
 * underlying CTMC with n states. Each state i corresponds to an arbitrary
 * interarrival process. 
 * 
 * Parameters expected in the config hash:
 * 
 * String ctmc        -- The configuration file for the CTMC.
 *  
 * String  process<i> -- The classname for the process used in state <i>.
 * String  config<i>  -- The configuration file for process<i>
 * 
 * 
 * @author Philipp Reinecke, Hewlett Packard Labs Bristol, philipp.reinecke@hpe.com or philipp.reinecke@fu-berlin.de
 *
 */

public class MarkovModulatedProcess extends Thread implements InterarrivalProcess {

	Random prng = new Random();
	int state;
	InterarrivalProcess[] processes;
	double[] intensities;
	
	CTMC ctmc;
	
	boolean isStarted = false;
	
	@Override
	public void configure(Hashtable<String, String> config) {

		ctmc = new CTMC();
		ctmc.configure(Util.readConfigFile(config.get("ctmc")));
		ctmc.init();

		int numStates = ctmc.getSize();
		processes = new InterarrivalProcess[numStates];

		for (int i=0; i<numStates; i++) {
			if (config.containsKey("process" + i) && 
				config.containsKey("config" + i)) {
				
				try {
					String processName = config.get("process" + i);
					String configName  = config.get("config" + i);
					
					processes[i] = (InterarrivalProcess) Util.loadModule(processName);
					processes[i].configure(Util.readConfigFile(configName));
					
				} catch (Exception e) {
					System.err.println("MarkovModulatedProcess: " + e);
					e.printStackTrace();
					System.exit(-1);
				}
				
			} else {
				System.err.println("MarkovModulatedProcess: Incomplete configuration.");
				System.exit(-1);
			}
		}
				
		start();
		
		
	}

	long endsleep = 0;
	
	public void run() {
		
		state = ctmc.getState();
		
		try {
			double sleepytime = ctmc.getSample();
			sleep((int) sleepytime);
		} catch (InterruptedException ie) {
			
		} catch (Exception e) {
			System.err.println(e);
			e.printStackTrace();
			System.exit(-1);
		}
		
		while (! Util.exitAllThreads) {
			
			try {
				int delayMillis;
				synchronized (this) {
					ctmc.nextState();
					state = ctmc.getState();
					double sleepytime = ctmc.getSample();

					delayMillis = (int) sleepytime;
					endsleep = System.currentTimeMillis() + delayMillis;

					//System.out.print("Sleeping for " + (sleepytime / 1000) + " seconds. ");

					sleepytime = delayMillis;
				}
				
				try {
					sleep(delayMillis);
				} catch (InterruptedException ie) {
					continue;
				}
				
				//System.out.println("State=" + state);
				
			} catch (Exception e) {
				System.err.println("Exception caught: " + e);
				e.printStackTrace();
				System.exit(-1);
			}
			
		}
		
	}
	
	@Override
	public Double getSample() {
		Double ret;
		
		synchronized (this) {
			ret = processes[state].getSample();
			//System.out.println("Got sample " + ret + " from state " + state);

			// FIXME:
			// Hack: Ensure that samples returned do not last longer than the
			// time left in the current state.
			if (ret + System.currentTimeMillis() > endsleep) {
				ret = (double) (endsleep - System.currentTimeMillis());
				if (ret < 0) {
					ret = 0.;
				}
			}
		}
		return ret;
	}

}
