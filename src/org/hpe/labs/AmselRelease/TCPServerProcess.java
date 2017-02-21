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
import java.net.InetAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.*;
import java.io.PrintWriter;

/**
 * A simple TCP server. It accepts all incoming data, and, upon receiving
 * a line of the format
 * 
 * GET <number>
 * 
 * returns a line containing of <number> characters (and a newline character).
 * 
 * Parameters expected in the configuration file:
 * 
 * String  address -- The address on which to listen.
 * Integer port    -- The port on which to listen. 
 * 
 * @author Philipp Reinecke, Hewlett Packard Labs Bristol, philipp.reinecke@hpe.com or philipp.reinecke@fu-berlin.de
 *
 */

public class TCPServerProcess extends Thread implements ServerProcess {

	ServerSocket server;
	
	@Override
	public void configure(Hashtable<String, String> config) {
		if (config.containsKey("address") &&
			config.containsKey("port"))
		{
			try {
				InetAddress address = InetAddress.getByName(config.get("address"));
				int port       = Integer.parseInt(config.get("port"));
				int backlog   = 50;
				
				server = new ServerSocket(port, backlog, address);
			} catch (Exception e) {
				System.err.println("TCPServerProcess: " + e);
				e.printStackTrace();
				System.exit(-1);
			}
			
		} else {
			System.err.println("TCPServerProcess: Incomplete configuration.");
			System.exit(-1);
		}
		
	}
	
	@Override
	public void run() {
		while (! Util.exitAllThreads) {
			try {
				System.out.println("Waiting for connections.");
				Socket socket = server.accept();
				System.out.println("Accepted new connection from " + socket);
				Worker w = new Worker(socket);
				w.start();
				
			} catch (IOException e) {
				System.err.println("TCPServerProcess: " + e);
				e.printStackTrace();
			}
		}
	}

	
	private class Worker extends Thread {

		Socket socket;

		public Worker(Socket socket) {
			this.socket = socket;
		}

		@Override
		public void run() {

			try {
				
				PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
				BufferedReader in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));

				while (! socket.isClosed() && ! Util.exitAllThreads) {

					String s = in.readLine();
					if (s == null) {
						break;
					}
					
					String[] request = s.split(" ");
					
					if (request[0].equals("GET")) {
						int size = Integer.parseInt(request[1]);

						StringBuilder sb = new StringBuilder();
						for (int i = 0; i < size; i++) {
							sb.append("b");
						}

						out.println(sb);

					} else {
					
					}
					
					//out.flush();
				}
				socket.close();
			} catch (Exception e) {
				// Ignore exceptions.
				System.err.println("Exception: " + e);
				e.printStackTrace();
				if (! socket.isClosed()) {
					try {
						socket.close();
					} catch (IOException ioe) {

					}
				}
			}
			
		}
	}

}

