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
 * A simple HTTP server. Supported methods:
 * 
 * 	GET   /get?length=<number> -- Returns a string of length <number>
 *  POST  /post                -- Accepts a string.  
 * 
 * Parameters expected in the configuration file:
 * 
 * String  address -- The address on which to listen.
 * Integer port    -- The port on which to listen. 
 * 
 * @author Philipp Reinecke, Hewlett Packard Labs Bristol, philipp.reinecke@hpe.com or philipp.reinecke@fu-berlin.de
 *
 */

import java.util.Hashtable;
import java.net.InetAddress;
import java.io.*;

import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class HttpServerProcess extends Thread implements ServerProcess {

	HttpServer server;
	
	@Override
	public void configure(Hashtable<String, String> config) {
		if (config.containsKey("address") &&
			config.containsKey("port"))
		{
			try {
				InetAddress address = InetAddress.getByName(config.get("address"));
				int port       = Integer.parseInt(config.get("port"));
				
				server = HttpServer.create(new InetSocketAddress(address, port), 0);
			    server.createContext("/get", new GetHandler());
			    server.createContext("/post", new PostHandler());
			    server.setExecutor(null);
			    server.start();
				
			} catch (Exception e) {
				System.err.println("HttpServerProcess: " + e);
				e.printStackTrace();
				System.exit(-1);
			}
			
		} else {
			System.err.println("HttpServerProcess: Incomplete configuration.");
			System.exit(-1);
		}
		
	}
	
	@Override
	public void run() {
		while (! Util.exitAllThreads) {
			try {
				Thread.sleep(60000);
			} catch (Exception e) {
				System.err.println("HttpServerProcess: " + e);
				e.printStackTrace();
			}
		}
		server.stop(3);
	}

	static class GetHandler implements HttpHandler {
	    
		public void handle(HttpExchange t) throws IOException {

			String query = t.getRequestURI().getQuery();
			if (query == null) {
				return;
			}
			// Parameter expected: length=<number>
			String parameter = query.split("&")[0].split("=")[1];
			int length = Integer.parseInt(parameter);
			
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < length-1; i++) {
				sb.append("b");
			}
			String response = sb.toString() + "\n";
			
			t.sendResponseHeaders(200, response.length());
			OutputStream os = t.getResponseBody();
			os.write(response.getBytes());
			os.close();
		}
		
	}
	
	static class PostHandler implements HttpHandler {
	    
		public void handle(HttpExchange t) throws IOException {
			
			t.sendResponseHeaders(200, "Thank you.".length());
			
			System.out.println("Got " + t.getRequestMethod());
			BufferedReader in = new BufferedReader(new InputStreamReader(t.getRequestBody()));
			String s;
			int length = 0;
			while ((s=in.readLine()) != null) {
				//System.out.println("Read: " + s);
				length += s.length();
			}
			System.err.println("Received " + length + " characters.");
			in.close();
			
			t.close();
		}
		
	}
	

}

