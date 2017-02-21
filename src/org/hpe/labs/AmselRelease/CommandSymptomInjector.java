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
import java.util.Random;
import java.util.Hashtable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.nio.file.StandardCopyOption;

import java.util.ArrayList;
import java.util.HashSet;

/*
 * The CommandSymptomInjector describes a state in which the behaviour of an attacker executing commands in a shell is emulated.
 * 
 * Please note that, depending on the commands, this can cause real damage, so use it wisely.
 *
 * The following parameters are expected in the configuration file:
 * 
 * TimeoutProcess             -- An interarrival process giving the timeouts for commands.       
 * 
 * Integer numberOfCommands   -- The number of commands. Each command 1 <= i <= numberOfCommands must be specified by a command<i>
 *                               parameter, and may also have a directory<i> parameter.
 * 
 * 
 * Optional parameters:
 * 
 * String  requiredFlag  -- A flag expected on the command line. If the flag is specified here, but not present on the command line, no encryption will take place.
 * Boolean randomise     -- Pick commands randomly (uniform distribution). If this flag is not set, commands are picked in sequence.  
 * 
 * @author Philipp Reinecke, Hewlett Packard Labs Bristol, philipp.reinecke@hpe.com or philipp.reinecke@fu-berlin.de
 * 
 * Created: 24 May 2016 07:05:46
 * 
 */

public class CommandSymptomInjector extends Thread implements HostSymptomGenerator{

	final static String[] requiredParameters = {
			"TimeoutProcess",
			"numberOfCommands",
	};
	
	final static String[] optionalParameters = {
			"requiredFlag",
	};

	InterarrivalProcess commandInterarrivals;
	InterarrivalProcess commandTimeouts;
		
	boolean generateSymptoms = false;
	boolean started = false;
	boolean exitThisThread = false;
	
	boolean executeCommands = true;
	boolean randomise = false;
	int commandIndex = 0;
	
	ProcessBuilder pb = new ProcessBuilder();
	HashSet<ProcessHandler> processHandlers = new HashSet<ProcessHandler>();
	
	Random prng = new Random();
	
	ArrayList<String> commands;
	//ArrayList<String> environments;
	File[] directories;
	
	boolean runningOnWindows = false;
	
	@Override
	public void configure(Hashtable<String, String> config) {

		if (Util.checkConfigRequired(this, config, requiredParameters)) {

			try {
				
				int numberOfCommands = Integer.parseInt(config.get("numberOfCommands"));
				
				commands = new ArrayList<String>();
				directories = new File[numberOfCommands]; 
				
				for (int i=1; i<=numberOfCommands; i++) {
					if (config.containsKey("command" + i)) {
						commands.add(config.get("command" + i));
						
						if (config.containsKey("directory" + i)) {
							directories[i - 1] = new File(config.get("directory" + i));
						} else {
							directories[i - 1] = null;
						}
					} else {
						System.err.println(this.getClass().getName() + ".configure(): Parameter `command " + i + "' missing.");
						System.exit(-1);
					}
				}
				
				commandTimeouts = (InterarrivalProcess) Util.loadModule(config.get("TimeoutProcess"));
				commandTimeouts.configure(Util.readConfigFile(config.get("TimeoutProcessConfig")));
				
				if (config.containsKey("requiredFlag")) {
					
					String requiredFlag = config.get("requiredFlag").trim();
					
					if (! Util.cmdlineFlags.contains(requiredFlag)) {
						executeCommands = false;
					}
				}
				
				if (config.containsKey("randomise")) {
					randomise = Util.parseBoolean(config.get("randomise"));
				}
				
				if (System.getProperty("os.name").toLowerCase().indexOf("windows") > -1) {
					runningOnWindows = true;
				} else {
					runningOnWindows = false;
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
				yield();
				continue;
			}
			
			runNextCommand();
			
			double delay = commandInterarrivals.getSample();
			int delayMillis = (int) delay;
			
			try {
				sleep(delayMillis);
			} catch (InterruptedException ie){
				continue;
			}
		}
		
		// Stop all sub-processes.
		for (ProcessHandler ph : processHandlers) {
			ph.interrupt();
		}
		
	}
	
	private void runNextCommand() {

		if (randomise) {
			commandIndex = prng.nextInt(commands.size());
		}
		
		//pb.command(Util.parseStringArray(commands.get(commandIndex)));
		
		if (runningOnWindows) {
			pb.command(new String[] {"cmd.exe", "/C", commands.get(commandIndex)});
		} else {
			pb.command(new String[] {"/bin/sh", "-c", commands.get(commandIndex)});	
		}
		
		pb.directory(directories[commandIndex]);
		
		int timeout = (int) (double) commandTimeouts.getSample();
		
		try {
			pb.inheritIO();
			Process p = pb.start();
			
			ProcessHandler ph = new ProcessHandler(p, timeout);
			processHandlers.add(ph);
			ph.start();
			
		} catch (IOException ioe) {
			System.err.println("IOException caught when executing " + commands.get(commandIndex) + ": " + ioe + "\nContinuing.");
		}
		
		if (! randomise) {
			commandIndex++;
			commandIndex = commandIndex < commands.size() ? commandIndex : 0;
		}
		
	}
	
	@Override
	public void registerInterarrivalProcess(InterarrivalProcess p) {
		commandInterarrivals = p;
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
