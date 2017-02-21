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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.io.File;
import java.util.LinkedList;
import java.util.Hashtable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.nio.file.StandardCopyOption;

/**
 * The RansomwareSymptomInjector describes a state where ransomware is emulated.
 * 
 * The following parameters are expected in the configuration file:
 * 
 * BlockAccessInterarrivalProcess -- The interarrival process for successive block accesses to one file.
 * BlockAccessInterarrivalProcessConfig
 * 
 * Integer blockSize     -- The block size at which to read files.
 * Double  processDelay  -- The delay per block (in seconds).
 * 
 * String List directories
 * String namePatterns   -- File-name patterns expressed in PathMatcher format.
 * 
 * Optional parameters:
 * 
 * Boolean encrypt       -- A flag specifying whether or not to emulate encryption. If set to true, the parameter `key' is also required.
 * Byte    key           -- A byte value used in encryption. Encryption is a simple byte-wise XOR with key.
 * String  requiredFlag  -- A flag expected on the command line. If the flag is specified here, but not present on the command line, no encryption will take place.  
 * 
 * @author Philipp Reinecke, Hewlett Packard Labs Bristol, philipp.reinecke@hpe.com or philipp.reinecke@fu-berlin.de
 *
 */

public class RansomwareSymptomInjector extends Thread implements HostSymptomGenerator{

	final static String[] requiredParameters = {
			"BlockAccessInterarrivalProcess",
			"BlockAccessInterarrivalProcessConfig",
			"blockSize",
			"processDelay",
			"directories",
			"namePatterns",
	};
	
	final static String[] optionalParameters = {
			"encrypt",
			"key",
			"requiredFlag",
	};

	InterarrivalProcess faInterarrivals;
	InterarrivalProcess blockInterarrivals;
	int blockSize;
	long processDelayNanos;
	
	String[] directories;
	PathMatcher namePatterns;
	
	boolean encrypt = false;
	byte key = 0;
	
	boolean generateSymptoms = false;
	boolean started = false;
	boolean exitThisThread = false;
	
	
	
	@Override
	public void configure(Hashtable<String, String> config) {

		if (Util.checkConfigRequired(this, config, requiredParameters)) {

			try {
				blockInterarrivals = (InterarrivalProcess) Util.loadModule(config.get("BlockAccessInterarrivalProcess"));

				blockSize = Integer.parseInt(config.get("blockSize"));

				double processDelay = Double.parseDouble(config.get("processDelay"));
				processDelayNanos = (long) (processDelay * 10e09);

				directories = Util.parseStringArray(config.get("directories"));
				
				files = new LinkedList<File>();
				
				for (String d : directories) {
					files.addLast(new File(d));
				}

				namePatterns = FileSystems.getDefault().getPathMatcher(config.get("namePatterns"));
				
				if (config.containsKey("encrypt") && Util.parseBoolean(config.get("encrypt"))) {
					if (! config.containsKey("key")) {
						System.err.println(this.getClass().getName() + ": Parameter `key' must be specified when `encrypt = true'.");
						System.exit(-1);
					}
					encrypt = Util.parseBoolean(config.get("encrypt"));
					key = Byte.parseByte(config.get("key"));
					 
				}
				
				if (config.containsKey("requiredFlag")) {
					
					String requiredFlag = config.get("requiredFlag").trim();
					
					if (! Util.cmdlineFlags.contains(requiredFlag)) {
						encrypt = false;
					}
				}
				
			} catch (Exception e) {
				System.err.println(this.getClass().getName() + ".configure(): Exception caught: " + e);
				e.printStackTrace(System.err);
				System.exit(-1);
			}
			
		} else {
			System.exit(-1);
		}
		
	}
	
	public void run() {
		
		while (! Util.exitAllThreads && ! exitThisThread) {
			
			if (! generateSymptoms) {
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
				}
				continue;
			}
			
			encryptNextFile();
			
			double delay = faInterarrivals.getSample();
			int delayMillis = (int) delay;
			
			try {
				sleep(delayMillis);
			} catch (InterruptedException ie){
				continue;
			}
		}
		
	}
	
	private LinkedList<File> files;
	
	private void encryptNextFile() {

		if (files.size() == 0) {
			return;
		}
		File current = files.removeFirst();
		if (current.isDirectory()) {
			for (File f : current.listFiles()) {
				System.out.println("Adding " + f.getPath());
				files.addLast(f);
			}
		} else {
			System.out.println("Looking at " + current.toPath());
			if (namePatterns.matches(current.toPath())) {
				System.out.println("Encrypting " + current.getPath());
				encrypt(current);
				System.out.println("Done");
			}
		}
	}
	
	private void encrypt(File f) {
		InputStream inStream = null;
		OutputStream outStream = null;
		try{
			File output = null;

			try {
				byte[] bucket = new byte[blockSize];
				inStream = new BufferedInputStream(new FileInputStream(f));

				output = File.createTempFile(f.getName(), "temp", new File(f.getParent()));

				System.out.println("Encrypting to " + output.getName());

				outStream = new BufferedOutputStream(new FileOutputStream(output));
				int bytesRead = 0;
				while(bytesRead != -1){
					bytesRead = inStream.read(bucket); //-1, 0, or more
					busyWait(processDelayNanos);
					
					if (encrypt) {
						for (int i = 0; i < bytesRead; i++) {
							bucket[i] = (byte) (bucket[i] ^ key);
						}
					}
					
					if (bytesRead > 0){
						outStream.write(bucket, 0, bytesRead);
					}
					int delayMillis = (int) blockInterarrivals.getSample().doubleValue();
					try {
						sleep(delayMillis);
					} catch (InterruptedException ie){
						continue;
					}
				}
			}
			finally {
				if (inStream != null) inStream.close();
				if (outStream != null) outStream.close();
			}

			Files.move(output.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);


		}
		catch (Exception e){
			System.err.println("Exception whilst encrypting " + f.getName() + ": " + e);
		}
	}
	
	private void busyWait(long delay) {
		long start = System.nanoTime();
		while(start + delay >= System.nanoTime()) ;
	}
	
	@Override
	public void registerInterarrivalProcess(InterarrivalProcess p) {
		faInterarrivals = p;
	}



	@Override
	public void startGenerator() {
		generateSymptoms = true;
		if (! started) {
			started = true;
			start();
		}
	}



	@Override
	public void stopGenerator() {
		generateSymptoms = false;
		this.interrupt();
	}

}
