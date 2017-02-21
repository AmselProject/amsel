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

public class SimpleServerBehaviour extends Thread implements OperationalPhasesProcess {

	boolean exitThisThread = false;
	ServerProcess serverProcess;
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while (! exitThisThread && ! Util.exitAllThreads) {
			try {
				Thread.sleep(60000);
			} catch (InterruptedException ie) {
				continue;
			}
		}
			
	}

	@Override
	public void configure(Hashtable<String, String> config) {
		if (config.containsKey("ServerProcess") &&
			config.containsKey("ServerProcessConfig"))
		{
			try {

				serverProcess = (ServerProcess) Util.loadModule(config.get("ServerProcess"));
				serverProcess.configure(Util.readConfigFile(config.get("ServerProcessConfig")));
			
			} catch (Exception e) {
				System.err.println("SimpleServerBehaviour.configure(): " + e);
				e.printStackTrace();
				System.exit(-1);
			}
			
			
		} else {
			System.err.println("SimpleServerBehaviour: Incomplete configuration.");
			System.exit(-1);
		}
	}

	@Override
	public void startBehaviour() {
		start();
		serverProcess.start();
	}

	@Override
	public void stopBehaviour() {
		this.interrupt();
	}

}
