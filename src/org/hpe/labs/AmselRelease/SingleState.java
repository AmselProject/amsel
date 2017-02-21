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

public class SingleState extends Thread implements OperationalPhasesProcess {

	private final static String[] requiredParameters = new String[] {
			"StateBehaviour",
			"StateBehaviourConfig",
	};
	
	private StateBehaviour sb;
	private boolean exitThisThread = false;
	
	@Override
	public void run() {
		while (!exitThisThread && ! Util.exitAllThreads) {
			try {
				sleep(60*1000);
			} catch (InterruptedException ie) {
				continue;
			}
		}
	}

	@Override
	public void configure(Hashtable<String, String> config) {
		if (! Util.checkConfigRequired(this, config, requiredParameters)) {
			System.exit(-1);
		}
		
		try {

			sb = (StateBehaviour) Util.loadModule(config.get("StateBehaviour"));
			sb.configure(Util.readConfigFile(config.get("StateBehaviourConfig")));

		} catch (Exception e) {
			System.err.println("SingleStateBehaviour.configure(): " + e);
			e.printStackTrace();
			System.exit(-1);
		}

	}

	@Override
	public void startBehaviour() {
		sb.startBehaviour();
	}

	@Override
	public void stopBehaviour() {
		exitThisThread = true;
		this.interrupt();
		sb.stopBehaviour();
		
	}

}
