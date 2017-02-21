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

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.util.Hashtable;

/**
 * A traffic generator for UDP traffic.
 * 
 * @author Philipp Reinecke, Hewlett Packard Labs Bristol, philipp.reinecke@hpe.com or philipp.reinecke@fu-berlin.de
 *
 */

public class UDPTrafficGenerator extends Thread implements TrafficGenerator {

	InterarrivalProcess arrivals;
	PacketSizeProcess sizes;
	AddressProcess addresses;

	boolean generateTraffic;
	boolean started = false;
	
	DatagramSocket socket;
	
	@Override
	public void configure(Hashtable<String, String> config) {
		// Nothing to configure so far.
	}
	
	public UDPTrafficGenerator() {
		generateTraffic = false;
		
		try {
			socket = new DatagramSocket(null); 
		} catch (SocketException se) {
			System.err.println("UDPTrafficGenerator: Exception during start-up:");
			se.printStackTrace();
			System.exit(-1);
		}
	}
	
	public void run() {
		while (! Util.exitAllThreads) {
			
			if (! generateTraffic) {
				yield();
			}
			
			double delay = arrivals.getSample();
			int delayMillis = (int) delay;
			
			try {
				sleep(delayMillis);
			} catch (InterruptedException ie){
				continue;
			}
			
			int size = sizes.getSample();
			byte[] buf = new byte[size];
			InternetAddress address = addresses.getSample();
			
			DatagramPacket packet = new DatagramPacket(buf, size, address.addr, address.port);
			
			try {
				socket.send(packet);
			} catch (Exception ioe) {
				System.err.println("UDPTrafficGenerator: IOException when sending packet:");
				ioe.printStackTrace();
				System.exit(-1);
			}
			
		}
	}
	
	@Override
	public void registerInterarrivalProcess(InterarrivalProcess p) {
		this.arrivals = p;
	}

	@Override
	public void registerPacketSizeProcess(PacketSizeProcess p) {
		this.sizes = p;
	}

	@Override
	public void registerAddressProcess(AddressProcess p) {
		this.addresses = p;
	}

	@Override
	public void registerTypeProcess(TypeProcess p) {
		if (p != null) {
			System.err.println("UDPTrafficGenerator: WARNING: This generator does not use a TypeProcess. There is no point in registering one.");
		}
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
