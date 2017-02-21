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
 * 
 * A simple HTTPS server. Supported methods:
 * 
 * 	GET   /get?length=<number> -- Returns a string of length <number>
 *  POST  /post                -- Accepts a string.  
 * 
 * Parameters expected in the configuration file:
 * 
 * String  address -- The address on which to listen.
 * Integer port    -- The port on which to listen. 
 * 
 * keytool -genkey -alias alias -keypass keystorePassword -keystore keystoreFile -storepass keystorePassword
 * 
 * @author Philipp Reinecke, Hewlett Packard Labs Bristol, philipp.reinecke@hpe.com or philipp.reinecke@fu-berlin.de
 *
 *
 */

import java.security.KeyStore;
import java.util.Hashtable;
import java.net.InetAddress;
import java.io.*;

import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

/**
 * HTTPS-specific code is based on: http://stackoverflow.com/questions/2308479/simple-java-https-server
 * 
 * Use a command-line like this for generating the certificates:
 * 
 * keytool -genkey -alias alias -keypass simulator -keystore lig.keystore -storepass simulator
 * 
 * @author Philipp Reinecke, Hewlett Packard Labs Bristol, philipp.reinecke@hpe.com or philipp.reinecke@fu-berlin.de
 *
 */


public class HttpsServerProcess extends Thread implements ServerProcess {

	HttpsServer server;
	
	final static String[] parameters = new String[] {
		//"InterarrivalProcess",
		//"PacketSizeProcess", 
		//"AddressProcess",
		//"TypeProcess",
		"address",
		"port",
		"keystoreFile",
		"keystorePassword",
	};
	
	@Override
	public void configure(Hashtable<String, String> config) {
		if (Util.checkConfigRequired(this, config, parameters))
		{
			try {
				InetAddress address = InetAddress.getByName(config.get("address"));
				int port            = Integer.parseInt(config.get("port"));
				
				server = HttpsServer.create(new InetSocketAddress(address, port), 0);
				
				setupSSL(config);
				
			    server.createContext("/get", new GetHandler());
			    server.createContext("/post", new PostHandler());
			    server.setExecutor(null);
			    server.start();
				
			} catch (Exception e) {
				System.err.println("HttpsServerProcess: " + e);
				e.printStackTrace();
				System.exit(-1);
			}
			
		} else {
			System.err.println("HttpsServerProcess: Incomplete configuration.");
			System.exit(-1);
		}
		
	}
	
	private void setupSSL(Hashtable<String, String> config) {
		try {
			// Load the keystore.
			char[] password = config.get("keystorePassword").toCharArray();
			KeyStore ks = KeyStore.getInstance("JKS");
			FileInputStream fis = new FileInputStream(Util.getFile(config.get("keystoreFile")));
			ks.load(fis, password);
			fis.close();

			// Set up the key-manager factory.
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(ks, password);

			// Set up the trust manager factory
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
			tmf.init(ks );

			// Set up the HTTPS context and parameters.
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(kmf.getKeyManagers (), tmf.getTrustManagers(), null);

			// Set up the configurator for SSL connections.
			server.setHttpsConfigurator (new HttpsConfigurator(sslContext) {
				public void configure (HttpsParameters params) {
					try
					{
						// Initialise the SSL context
						SSLContext c = SSLContext.getDefault();
						SSLEngine engine = c.createSSLEngine();
						params.setNeedClientAuth(false);
						params.setCipherSuites(engine.getEnabledCipherSuites());
						params.setProtocols(engine.getEnabledProtocols());

						// get the default parameters
						SSLParameters defaultSSLParameters = c.getDefaultSSLParameters();
						params.setSSLParameters(defaultSSLParameters);
					}
					catch (Exception ex)
					{
						System.err.println("HttpsServerProcess: " + ex);
						ex.printStackTrace();
					}
				}
			});
		} catch (Exception e) {
			System.err.println("HttpsServerProcess: " + e);
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	@Override
	public void run() {
		while (! Util.exitAllThreads) {
			try {
				Thread.sleep(60000);
			} catch (Exception e) {
				System.err.println("HttpsServerProcess: " + e);
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

