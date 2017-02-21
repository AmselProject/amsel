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
 * A traffic generator for a single TCP connection. It establishes the
 * connection and transfers data according to the given processes.
 * 
 * When used in the context of generating different TCP streams, this generator
 * should inherit the parent's address generator.
 * 
 * The TypeProcess of this generator should be a DirectionTypeProcess and return
 * the strings `IN' or `OUT'.
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

public class TCPTrafficGenerator extends Thread implements TrafficGenerator {

	// TODO: Generalise TrafficGenerators such that they also read their configuration
	// from a file, instead of using the register* methods. This may include having a
	// parent TrafficGenerator class to derive other generators from.
	final static String[] parameters = new String[] {
			//"InterarrivalProcess",
			//"PacketSizeProcess", 
			//"AddressProcess",
			//"TypeProcess",
			"reuseConnection",
	};
	
	InterarrivalProcess arrivals;
	PacketSizeProcess sizes;
	AddressProcess addresses;
	TypeProcess<String> types;

	boolean generateTraffic;
	boolean started = false;
	boolean exitThisThread = false;
	boolean reuseConnection;
	
	Socket socket = null;
	PrintWriter out;
	BufferedReader in;

	@Override
	public void configure(Hashtable<String, String> config) {
		if (Util.checkConfigRequired(this, config, parameters))
		{
			this.reuseConnection = Boolean.parseBoolean(config.get("reuseConnection"));
		} else {
			System.exit(-1);
		}
	}
	
	public TCPTrafficGenerator() {
		generateTraffic = false;
		
	}
	
	public void run() {
		
		
		
		while (! Util.exitAllThreads && ! exitThisThread) {
			
			if (! generateTraffic) {
				yield();
				continue;
			}
			
			try {

				if (socket == null) {
					InternetAddress address = addresses.getSample();
					
					socket = new Socket(address.addr, address.port);
					
					if (reuseConnection) {
						socket.setKeepAlive(true);
					}

					out = new PrintWriter(socket.getOutputStream(), true);
					in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));	

				}

				int size = sizes.getSample();

				String direction = types.getSample();

				if (direction.equals("IN")) {
					requestData(size);
				} else {
					sendData(size);
				}
				
				if (! reuseConnection) {
					System.out.println("Closing connection.");
					socket.close();
					socket = null;
				}

			} catch (IOException ioe) {
				System.err.println(this + ": IOE received " + ioe);
				ioe.printStackTrace();
			}

			double delay = arrivals.getSample();
			int delayMillis = (int) delay;
			
			try {
				sleep(delayMillis);
			} catch (InterruptedException ie){
				continue;
			}

			
		}
		
		try {
			socket.close();
		} catch (Exception e) {
			
		}
	}
	
	private void sendData(int size) throws IOException
	{
		// TODO: Generate string according to some model.
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < size; i++) {
			sb.append("a");
		}
		
		out.println(sb.toString());
		
	}
	
	private void requestData(int size) throws IOException
	{
		out.println("GET " + size);
		out.flush();
		in.readLine();
		
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
		this.types = (DirectionTypeProcess) p;
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
