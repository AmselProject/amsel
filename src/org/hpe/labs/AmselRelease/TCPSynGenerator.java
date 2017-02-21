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
 * A traffic generator for TCP SYN traffic. The generator attempts a connection and
 * immediately closes it. 
 * 
 * @author Philipp Reinecke, Hewlett Packard Labs Bristol, philipp.reinecke@hpe.com or philipp.reinecke@fu-berlin.de
 *
 */

import java.net.*;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Hashtable;

public class TCPSynGenerator extends Thread implements TrafficGenerator {

	// TODO: Generalise TrafficGenerators such that they also read their configuration
	// from a file, instead of using the register* methods. This may include having a
	// parent TrafficGenerator class to derive other generators from.
	final static String[] parameters = new String[] {
			"parallelAttempts",
	};
	
	AddressProcess addresses;
	InterarrivalProcess arrivals;
	
	boolean generateTraffic;
	boolean started = false;
	boolean exitThisThread = false;
	
	boolean parallelAttempts;

	@Override
	public void configure(Hashtable<String, String> config) {
		if (Util.checkConfigRequired(this, config, parameters))
		{
			this.parallelAttempts = Util.parseBoolean(config.get("parallelAttempts"));
		} else {
			System.exit(-1);
		}
	}
	
	public TCPSynGenerator() {
		generateTraffic = false;
		
	}
	
	public void run() {

		while (! Util.exitAllThreads && ! exitThisThread) {

			if (! generateTraffic) {
				yield();
				continue;
			}


			OneShot os = new OneShot(addresses.getSample());

			os.run();

			if (parallelAttempts) {
				try {
					os.join();
				} catch (InterruptedException ie) {
					continue;
				}
			}


			double delay = arrivals.getSample();
			int delayMillis = (int) delay;

			try {
				sleep(delayMillis);
			} catch (InterruptedException ie){
				continue;
			}

		}
	}
	
	private class OneShot extends Thread {
		InternetAddress address;
		
		public OneShot(InternetAddress address) {
			this.address = address;
		}
		
		@Override
		public void run() {
			try {
				Socket socket = new Socket(address.addr, address.port);
				socket.close();
			} catch (IOException e) {
				
			}
		}
	}
	
	@Override
	public void registerInterarrivalProcess(InterarrivalProcess p) {
		this.arrivals = p;
	}

	@Override
	public void registerPacketSizeProcess(PacketSizeProcess p) {
	}

	@Override
	public void registerAddressProcess(AddressProcess p) {
		this.addresses = p;
	}

	@Override
	public void registerTypeProcess(TypeProcess p) {
	}

	@Override
	public void startTraffic() {
		generateTraffic = true;
		if (! started) {
			started = true;
			start();
		}
	}
	
	@Override
	public void stopTraffic() {
		generateTraffic = false;
		this.interrupt();
	}
	
}
