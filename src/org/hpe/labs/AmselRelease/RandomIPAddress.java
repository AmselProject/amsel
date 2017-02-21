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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Random;

/**
 * Creates a random IP address based on a given address. The new address is
 * defined as
 * <p>
 * 	new_address = (random_address AND ~mask) OR (address AND mask),
 * <p>
 * i.e. mask determines which bits are taken from address and which bits are
 * taken from random_address. For instance, with
 * <p>
 *  mask = 255.0.255.0<br>
 *  address = 192.0.168.0<br>
 * <p>
 * the second and fourth octet are determined by random choice, and the first
 * and third octets are 192 and 168, respectively.
 * <p>
 * 
 * Parameters expected in the configuration hash:
 * 
 * @param String  mask      -- Netmask
 * @param String  address   -- IP address
 * @param Integer port      -- The IP port number.
 * 
 * 
 * @param fallbackAddress -- The fallback address to be used if dryRun is true.   
 * 
 * Boolean dryRun         -- Unless this is false, the value of fallbackAddress will be returned.
 *  
 * @author Philipp Reinecke, Hewlett Packard Labs Bristol, philipp.reinecke@hpe.com or philipp.reinecke@fu-berlin.de
 *
 */

public class RandomIPAddress implements AddressProcess {

	Random prng = new Random();
	
	byte[] negmask;
	byte[] address;
	int port;
	
	boolean dryRun;
	InternetAddress fallback;
	
	@Override
	public void configure(Hashtable<String, String> config) {

		if (Util.checkConfigRequired(this, config, new String[] {"mask", "address", "port", "dryRun", "fallbackAddress"} )) {
		
			try {
				byte[] mask = Util.readOctets(config.get("mask"));
				address = Util.readOctets(config.get("address"));

				address = andOctets(address, mask);
				negmask = negOctets(mask);

				port = Integer.parseInt(config.get("port"));


				dryRun = Util.parseBoolean(config.get("dryRun"));

				byte[] fb = Util.readOctets(config.get("fallbackAddress"));
				fallback    = new InternetAddress(InetAddress.getByAddress(fb), port);
				
			} catch (Exception uhe) {
				System.err.println("RandomIPAddress: " + uhe);
				System.exit(-1);
			}

		} else {
			System.exit(-1);
		}
		
		System.out.println("mask=" + address);
	}
	
	@Override
	public InternetAddress getSample() {
		
		
		byte[] addr = new byte[address.length];
		prng.nextBytes(addr);
		
		
		addr = andOctets(addr, negmask);
		addr = orOctets(addr, address);
		
		InternetAddress ret = null;
		try {
			ret = new InternetAddress(InetAddress.getByAddress(addr), port);
			
			if (dryRun) {
				System.out.println(ret);
			
				ret = fallback;
			}
			
		} catch (UnknownHostException uhe) {
			System.err.println("Exception: " + uhe);
			//System.exit(-1);
			
			ret = fallback;
		}

		
		return ret;
		
	}
	
	
	
	private byte[] andOctets(byte[] a, byte[] b) {
		byte[] ret = new byte[a.length];
		for (int i=0; i < a.length; i++) {
			ret[i] = (byte) (a[i] & b[i]);
		}
		return ret;
	}
	
	private byte[] orOctets(byte[] a, byte[] b) {
		byte[] ret = new byte[a.length];
		for (int i=0; i < a.length; i++) {
			ret[i] = (byte) (a[i] | b[i]);
		}
		return ret;
	}
	
	private byte[] negOctets(byte[] a) {
		byte[] ret = new byte[a.length];
		for (int i=0; i < a.length; i++) {
			ret[i] = (byte) ((~a[i]) & 0xff);
		}
		return ret;
	}
	
	
			 

}
