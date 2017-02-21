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
 * Markov-modulated behaviour of traffic generators. Both states and transitions
 * can have behaviour associated with them. This enables the implementation of
 * Markov-Modulated Processes (MMPs) as well as that of Markovian Arrival Processes (MAPs),
 * and all sorts of horrid hybrids and grotesque goblins involving the two.
 * 
 * Parameters expected in the config file:
 * 
 * String ctmc -- The configuration file of the underlying CTMC.
 * 
 * StateBehaviour<i>        -- The behaviour in state i
 * StateBehaviourConfig<i>  -- The configuration file for state i
 * 
 * Optional:
 * 
 * TransitionBehaviour<i>,<j> -- The behaviour for transition between states i and j.
 * TransitionBehaviourConfig<i>,<j> -- The configuration file for the said transition.
 * 
 * @author Philipp Reinecke, Hewlett Packard Labs Bristol, philipp.reinecke@hpe.com or philipp.reinecke@fu-berlin.de
 * 
 * TODO: Make class with abstract timings, derive ContinuousMarkovModulatedBehaviour, DiscreteMarkovModulatedBehaviour, etc.
 *
 */

public class MarkovModulatedBehaviour extends Thread implements OperationalPhasesProcess {

	CTMC ctmc;

	/**
	 * Stores the behaviours for states and transitions.
	 */
	StateBehaviour[] stateBehaviours;
	TransitionBehaviour[][] transitionBehaviours;
	
	String me = null;
	
	@Override
	public void run() {
		while (! Util.exitAllThreads) {
			int oldState = ctmc.getState();
			Util.logTime(me + ": Leaving state " + oldState);
			
			stateBehaviours[oldState].stopBehaviour();
			ctmc.nextState();
			int newState = ctmc.getState();
			
			if (transitionBehaviours[oldState][newState] != null) {
				transitionBehaviours[oldState][newState].behave();
			}

			Util.logTime(me + ": Entering state " + ctmc.getState() + ". Sojourn time " + (ctmc.getSample() / 1000.0) + "s");
			
			stateBehaviours[newState].startBehaviour();
			try {
				sleep((long) (double) ctmc.getSample());
			} catch (InterruptedException ie) {
				continue;
			} catch (Exception e) {
				System.err.println("Exception: " + e);
				e.printStackTrace();
				System.exit(-1);
			}
			
		}
	}

	@Override
	public void configure(Hashtable<String, String> config) {
		
		me = this.getClass().getName();
				
		ctmc = new CTMC();
		ctmc.configure(Util.readConfigFile(config.get("ctmc")));
		
		stateBehaviours = new StateBehaviour[ctmc.getSize()];
		
		if (config.get("StateBehaviourConfig0") != null) {
			System.err.println("This configuration file starts state-numbering at 0. This is not supported anymore.");
			System.exit(-1);
		}
		
		for (int state = 1; state <= ctmc.getSize(); state++) {
			
			System.out.println("Configuring state " + state + " of " + ctmc.getSize());
			
			try {
				
				StateBehaviour sb = (StateBehaviour) Util.loadModule(config.get("StateBehaviour" + state));
				sb.configure(Util.readConfigFile(config.get("StateBehaviourConfig" + state)));
				
				stateBehaviours[state-1] = sb;
				
				
			} catch (Exception e) {
				System.err.println("MarkovModulatedBehaviour.configure(): " + e);
				e.printStackTrace();
				System.exit(-1);
			}
			
		}
		
		transitionBehaviours = new TransitionBehaviour[ctmc.getSize()][ctmc.getSize()];
		
		for (int i = 1; i <= ctmc.getSize(); i++) {
			for (int j = 1; j <= ctmc.getSize(); j++) {
				if (i == j) {
					continue;
				}
				
				if (config.get("TransitionBehaviour" + i + "," + j) != null) {

					System.out.println("Configuring transition " + i + " -> " + j);
					
					try {
					
						TransitionBehaviour tb = (TransitionBehaviour) Util.loadModule(config.get("TransitionBehaviour" + i + "," + j));
						tb.configure(Util.readConfigFile(config.get("TransitionBehaviourConfig" + i + "," + j)));
				
						transitionBehaviours[i-1][j-1] = tb;
					} catch (Exception e) {
						System.err.println("MarkovModulatedBehaviour.configure(): " + e);
						e.printStackTrace();
						System.exit(-1);
					}
				}
				
			}
			
		}
		
		System.out.flush();
		
	}

	public void startBehaviour() {
		ctmc.init();
		Util.logTime(me + ": Behaviour state " + ctmc.getState() + " for " + (ctmc.getSample() / 1000.0) + "s");
		
		stateBehaviours[ctmc.getState()].startBehaviour();
		try {
			sleep((long) (double) ctmc.getSample());
		} catch (Exception e) {
			System.err.println("Exception: " + e);
			e.printStackTrace();
			System.exit(-1);
		}
		start();
	}

	public void stopBehaviour() {
		this.interrupt();
		
		for (int i = 0; i < stateBehaviours.length; i++) {
			stateBehaviours[i].stopBehaviour();
		}

	}
	
	

}
