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


import java.lang.reflect.Field;

/**
 * 
 * Simple handler for a single process. For now, this class only kills the process after
 * the specified timeout has elapsed. Future implementations may also support feeding the
 * process with input, and consuming its output.
 * 
 * @author Philipp Reinecke, Hewlett Packard Labs Bristol, philipp.reinecke@hpe.com or philipp.reinecke@fu-berlin.de
 *
 * Created: 24 May 2016 08:42:19
 *
 */


public class ProcessHandler  extends Thread {

	Process p;
	int timeout;
	int pid;
	
	public ProcessHandler(Process p, int timeout) {
		this.p = p;
		this.timeout = timeout;

		// Get PID if on Linux.
		if (System.getProperty("os.name").toLowerCase().indexOf("windows") == -1) {
			try {
				Field f = p.getClass().getDeclaredField("pid");
				f.setAccessible(true);
				pid = (Integer) f.get(p);
			} catch (Exception e) {
				System.err.println("Exception caught whilst extracting PID: " + e);
				System.exit(-1);
			}
		} else {
			System.err.println("Cannot kill task on Windows systems in this version.");
		}
	}
	
	@Override
	public void run() {

		System.err.println("Waiting for " + timeout + " ms.");
		
		try {
			Thread.sleep(timeout);
		} catch (InterruptedException ie) {
			
		}
		
		
		
		try {
			// Java 7 on the testbed client does not seem to support p.isAlive().
			//Runtime.getRuntime().exec(new String[] {"pkill" , "-P", "" + pid} );

			System.out.println("Trying to kill " + pid);
			
			if (System.getProperty("os.name").toLowerCase().indexOf("windows") > -1) {
				System.err.println("Cannot kill task on Windows systems in this version.");				
			} else {
				//if (p.isAlive()) {
				System.out.println("Executing pkill.");
				Runtime.getRuntime().exec(new String[] {"pkill" , "-9", "-P", "" + pid} );
				//}
			}
		} catch (Exception e) {
			System.err.println("Exception caught whilst trying to kill " + pid + ": " + e);
			System.exit(-1);
		}

	}
	
}
