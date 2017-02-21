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
 * A traffic generator for a single Http connection. It establishes the
 * connection and transfers data according to the given processes.
 * 
 * When used in the context of generating different Http streams, this generator
 * should inherit the parent's address generator.
 * 
 * The TypeProcess of this generator should be a DirectionTypeProcess and return
 * the strings `IN' or `OUT'.
 *
 * Keystore generation:
 * 
 * keytool -keystore keystore.server -alias alias -exportcert -file tmp.cert
 * keytool -keystore keystore.client -alias alias -importcert -file tmp.cert
 * 
 * @author Philipp Reinecke, Hewlett Packard Labs Bristol, philipp.reinecke@hpe.com or philipp.reinecke@fu-berlin.de
 *
 */

import java.net.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.KeyStore;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.io.*;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.SSLSession;

public class HttpsTrafficGenerator extends Thread implements TrafficGenerator {

	// TODO: Generalise TrafficGenerators such that they also read their configuration
	// from a file, instead of using the register* methods. This may include having a
	// parent TrafficGenerator class to derive other generators from.
	final static String[] requiredParameters = new String[] {
			"keystoreFile",
			"keystorePassword",
	};
	
	final static String[] optionalParameters = new String[] {
			// TODO: Implement support for maxAttempts and individualDelay.
//			"maxAttempts",
//			"individualDelay",
			"useProxy",
	};
	
	InterarrivalProcess arrivals;
	PacketSizeProcess sizes;
	AddressProcess addresses;
	TypeProcess<String> types;

	boolean generateTraffic;
	boolean started = false;
	boolean exitThisThread = false;
	boolean reuseConnection;
	boolean useProxy = false;
	
	int maxAttempts = 1;
	int individualDelay = 500;
	
	HttpURLConnection  connection = null;
	PrintWriter    out;
	BufferedReader in;
	
	String me = null;

	@Override
	public void configure(Hashtable<String, String> config) {
		if (Util.checkConfigRequired(this, config, requiredParameters))
		{
			me = this.getClass().getName();
			try {
				// Load keystore.
				char[] password = config.get("keystorePassword").toCharArray ();
				KeyStore ks = KeyStore.getInstance ("JKS");
				FileInputStream fis = new FileInputStream (Util.getFile(config.get("keystoreFile")));
				ks.load (fis, password);
				fis.close();
				
				// Set up the key manager factory.
				KeyManagerFactory kmf = KeyManagerFactory.getInstance ("SunX509");
				kmf.init (ks, password);

				// Set up the trust manager factory.
				TrustManagerFactory tmf = TrustManagerFactory.getInstance ("SunX509");
				tmf.init (ks);

				// Set up the HTTPS context and parameters.
				SSLContext sslContext = SSLContext.getInstance ("TLS");
				sslContext.init (kmf.getKeyManagers (), tmf.getTrustManagers (), null);

				// Set up SocketFactory for later connections.
				HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

		        // Create all-trusting host name verifier.
				// TODO: This is a hack to make things work. Ideally, this would not be needed. 
		        HostnameVerifier allHostsValid = new HostnameVerifier() {
		            public boolean verify(String hostname, SSLSession session) {
		                return true;
		            }
		        };
		        
		        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
		        
		        HashSet<String> optionalParametersPresent = Util.checkConfigOptional(this, config, optionalParameters);
				
				if (optionalParametersPresent.contains("maxAttempts")) {
					maxAttempts = Integer.parseInt(config.get("maxAttempts"));
				}
				if (optionalParametersPresent.contains("individualDelay")) {
					individualDelay = Integer.parseInt(config.get("individualDelay"));
				}
				if (optionalParametersPresent.contains("useProxy")) {
					useProxy = Util.parseBoolean(config.get("useProxy"));
				}
				
			} catch (Exception e) {
				System.err.println("HttpsTrafficGenerator.configure(): " + e);
				e.printStackTrace();
				System.exit(-1);
			}
			
		} else {
			System.exit(-1);
		}
	}
	
	public HttpsTrafficGenerator() {
		generateTraffic = false;
		
	}
	
	public void run() {
		
		while (! Util.exitAllThreads && ! exitThisThread) {
			
			//if (! generateTraffic) {
//				yield();
				//continue;
			//}
			
			if (! generateTraffic) {
				//yield();
				try {
					Thread.sleep(10000);
				} catch (InterruptedException ie) {
				}
				continue;
			}
			
			try {

				InternetAddress address = addresses.getSample();
				String direction = types.getSample();

				int size = sizes.getSample();
				
				if (! generateTraffic) {
					continue;
				}
				
				if (direction.equals("IN")) {
					requestData(address, size);
				} else {
					sendData(address, size);
				}

			} catch (IOException ioe) {
				System.err.println(this + ": IOE received " + ioe);
				ioe.printStackTrace();
				connection.disconnect();
				connection = null;
				//System.exit(-1);
			} catch (Exception e) {
				System.err.println(this + ": Exception received " + e);
				e.printStackTrace();
				connection.disconnect();
				connection = null;
			} finally {
				if (! reuseConnection) {
					System.out.println("Closing connection.");
					if (connection != null) {
						connection.disconnect();
						connection = null;
					}
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
		
		try {
			connection.disconnect();
		} catch (Exception e) {
			
		}
	}
	
	private void sendData(InternetAddress address, int size) throws Exception
	{
		// TODO: Generate string according to some model.
		
		URL url = new URL("https://" + address.addr.getHostAddress() + ":" + address.port + "/post");
		
		System.err.println("Sending to " + url);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < size; i++) {
			sb.append("a");
		}
		
		connection = (HttpsURLConnection) this.openConnection(url);
		connection.setDoOutput(true);
		connection.setRequestProperty("Content-Length", "" + size);
		
		OutputStream out = connection.getOutputStream();
		out.write(sb.toString().getBytes());
		out.flush();
		
		in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		while (in.ready()) {
			System.out.println("Read: " + in.readLine());
		}
		
		connection.disconnect();
		
	}
	
	private void requestData(InternetAddress address, int size) throws Exception
	{
		URL url = new URL("https://" + address.addr.getHostAddress() + ":" + address.port + "/get?length=" + size);
		
		System.err.println("Requesting data from " + url);
		
		connection = (HttpsURLConnection) url.openConnection();
		connection.setDoOutput(true);
		out = new PrintWriter(connection.getOutputStream(), true);
		out.flush();
		
		connection = (HttpsURLConnection) this.openConnection(url);
		connection.setDoInput(true);
		in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

		
		int length = 0;
		String read;
		while ((read = in.readLine()) != null) {
			length += read.length();
		}
		
		System.err.println("Read " + length + " characters.");
		
		connection.disconnect();
		
	}
	
	/**
	 * Open a connection -- via a proxy, if useProxy is true --, directly otherwise or if none of the proxies works.
	 *  
	 * @param url
	 * @return
	 * @throws IOException
	 */
	private HttpsURLConnection openConnection(URL url) throws IOException {
		List<Proxy> l = null;
		URI uri = null;
		try {
			uri = url.toURI();
		} catch (URISyntaxException e) {
			Util.logTime(me + " URISyntaxException " + e + " on URL " + url);
		}
		
		if (useProxy && uri != null) {
			try {
			  l = ProxySelector.getDefault().select(url.toURI());
			} catch (URISyntaxException e) {
			  e.printStackTrace();
			}
		}
		if (l != null) {
			for (Proxy proxy : l) {
				try {
					return (HttpsURLConnection) url.openConnection(proxy);
				} catch (IOException e) {
					Util.logTime(me + " IOException when attempting to connect via proxy " + proxy + ":" + e);
					ProxySelector.getDefault().connectFailed(uri, proxy.address(), e);
				}
			}
		}

		// Default behaviour if no proxy is used, no proxy was configured, or none of the proxies worked.
		return (HttpsURLConnection) url.openConnection();

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
		this.interrupt();
	}
	
	@Override
	public void stopTraffic() {
		generateTraffic = false;
		this.interrupt();
	}
	
}
