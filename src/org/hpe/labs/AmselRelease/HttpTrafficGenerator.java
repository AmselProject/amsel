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
 * A traffic generator for HTTP connections. It establishes the
 * connection and transfers data according to the given processes.
 * 
 * When used in the context of generating different Http streams, this generator
 * should inherit the parent's address generator.
 * 
 * The TypeProcess of this generator should be a DirectionTypeProcess and return
 * the strings `IN' or `OUT'.
 * 
 * @author Philipp Reinecke, Hewlett Packard Labs Bristol, philipp.reinecke@hpe.com or philipp.reinecke@fu-berlin.de
 *
 */

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.List;
import java.util.HashSet;

import java.net.URL;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;

public class HttpTrafficGenerator extends Thread implements TrafficGenerator {

	final static String[] requiredParameters = new String[] {
	};
	
	final static String[] optionalParameters = new String[] {
			"maxAttempts",
			"individualDelay",
			"useProxy",
	};
	
	InterarrivalProcess arrivals;
	PacketSizeProcess sizes;
	AddressProcess addresses;
	TypeProcess<String> types;

	boolean generateTraffic;
	boolean started = false;
	boolean exitThisThread = false;
	boolean useProxy = false;
	
	int maxAttempts = 1;
	int individualDelay = 100;
	
	String me = null;
	
	@Override
	public void configure(Hashtable<String, String> config) {
		me = this.getClass().getName();
		
		if (Util.checkConfigRequired(this, config, requiredParameters))
		{
			
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
			
			
		} else {
			System.exit(-1);
		}
	}
	
	public HttpTrafficGenerator() {
		generateTraffic = false;
		
	}
	
	public void run() {
		
		while (! Util.exitAllThreads && ! exitThisThread) {
			
			if (! generateTraffic) {

				//yield();
				try {
					synchronized(this) {
						this.wait();
					}
					//Thread.sleep(10000);
				} catch (InterruptedException ie) {
					System.out.println("Interrupted.");
				}
				System.out.println("Slept.");
				continue;
			}

			for (int j = 0; j < maxAttempts; j++) {

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
					System.err.println(this + ": IOE received " + ioe + ". There is probably no HTTP server on the host we are trying to access. Continuing.");
					//ioe.printStackTrace();
				}
				
				if (j < maxAttempts) {
					try {
						Thread.sleep(individualDelay);
					} catch (InterruptedException ie) {
						break;
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
		
	}
	
	/**
	 * Send a string of given size to the given address using HTTP POST. 
	 * 
	 * @param address
	 * @param size
	 * @throws IOException
	 */
	private void sendData(InternetAddress address, int size) throws IOException
	{
		// TODO: Generate string according to some model.
		
		URL url = new URL("http://" + address.addr.getHostAddress() + ":" + address.port + "/post");
		
		System.err.println("Sending to " + url);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < size; i++) {
			sb.append("a");
		}
		
		HttpURLConnection connection = (HttpURLConnection) this.openConnection(url);
		connection.setDoOutput(true);
		connection.setRequestProperty("Content-Length", "" + size);
		
		OutputStream out = connection.getOutputStream();
		out.write(sb.toString().getBytes());
		out.flush();
		
		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		while (in.ready()) {
			System.out.println("Read: " + in.readLine());
		}
		connection.disconnect();
		
	}
	
	/**
	 * Open a connection -- via a proxy, if useProxy is true --, directly otherwise or if none of the proxies works.
	 *  
	 * @param url
	 * @return
	 * @throws IOException
	 */
	private HttpURLConnection openConnection(URL url) throws IOException {
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
					return (HttpURLConnection) url.openConnection(proxy);
				} catch (IOException e) {
					Util.logTime(me + " IOException when attempting to connect via proxy " + proxy + ":" + e);
					ProxySelector.getDefault().connectFailed(uri, proxy.address(), e);
				}
			}
		}

		// Default behaviour if no proxy is used, no proxy was configured, or none of the proxies worked.
		return (HttpURLConnection) url.openConnection();

	}
	
	
	/**
	 * Request a response of given size from the server at the address using
	 * HTTP GET.
	 * 
	 * @param address
	 * @param size
	 * @throws IOException
	 */
	private void requestData(InternetAddress address, int size) throws IOException
	{
		URL url = new URL("http://" + address.addr.getHostAddress() + ":" + address.port + "/get?length=" + size);
		
		System.err.println("Requesting data from " + url);
		
		HttpURLConnection connection = (HttpURLConnection) this.openConnection(url);
		connection.setDoInput(true);
		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

		int length = 0;
		String read;
		while ((read = in.readLine()) != null) {
			length += read.length();
			//System.err.println("Read: " + in.readLine());
		}
		
		System.err.println("Read " + length + " characters.");
		
		connection.disconnect();
		
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
//		this.notifyAll();
	}
	
	@Override
	public void stopTraffic() {
		generateTraffic = false;
		this.interrupt();
	}
	
}
