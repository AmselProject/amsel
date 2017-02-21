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
import java.util.Arrays;
import java.util.Random;

/**
 * A utility class for simulating a CTMC.
 *
 * Parameters expected in the config hash:
 * 
 * Integer           numberOfStates -- the size n of the CTMC
 * Array of doubles  initialVector = <a1>, <a2>, ..., <a_n>  -- the initial probabilities
 * Double            rate_<i>,<j> -- Rate for transition from i to j. Transitions
 *                                   not specified are assumed to be zero.
 * 
 * @author Philipp Reinecke, Hewlett Packard Labs Bristol, philipp.reinecke@hp.com

 */

public class CTMC implements StochasticProcess<Double> {

	int numberOfStates;
	double[] initialVector;
	
	double[][] embeddedDTMC;
	double[] intensities;
	
	int currentState;
	double currentSample;
	
	Random prng = new Random();
	
	public void configure(Hashtable<String, String> config) {
		
		// Read number of states
		// Read transitions: rate_i,j
		// Read initial vector: initialVector = a1, a2, a3
		
		if (! Util.checkConfigRequired(this, config, new String[] {"numberOfStates", "initialVector"} )) {
			System.exit(-1);
		}
		
		numberOfStates = Integer.parseInt(config.get("numberOfStates"));
		initialVector = new double[numberOfStates];
		String[] siv = config.get("initialVector").split("\\s*,\\s*");
		for (int i = 0; i < numberOfStates; i++) {
			initialVector[i] = Double.parseDouble(siv[i]);
		}
		
		if (config.containsKey("configurationFileFormat") &&
				config.get("configurationFileFormat").equals("ExplicitDTMC"))
		{
			configureFromDTMC(config);
		} else {
			configureFromCTMC(config);
		}
		
		
	}

	public Double getSample() {
		return currentSample; 
	}
	
	public int getState() {
		return currentState;
	}
	
	public void nextState() {
		currentState = Util.discreteRandom(prng, embeddedDTMC[currentState]);
		currentSample = Util.exponentialRandom(prng, intensities[currentState]);
	}
	
	public void init() {
		currentState = Util.discreteRandom(prng, initialVector);
		currentSample = Util.exponentialRandom(prng, intensities[currentState]);
	}
	
	public int getSize() {
		return numberOfStates;
	}
	
	public String toString() {
		String ret = "[CTMC: \n";
		
		ret = ret + "initialVector=" + Arrays.toString(initialVector) + "\n";
		
		//for (int i = 0; i < numberOfStates; i++)
			//ret = ret + "generatorMatrix=" + Arrays.toString(generatorMatrix[i]) + "\n";
		
		for (int i = 0; i < numberOfStates; i++)
			ret = ret + "embeddedDTMC=" + Arrays.toString(embeddedDTMC[i]) + "\n";
		
		ret = ret + "intensities=" + Arrays.toString(intensities) + "\n";

		ret = ret + "]\n";
		
		return ret;
	}
	
	private void configureFromCTMC(Hashtable<String, String> config) {
		
		double[][] generatorMatrix = new double[numberOfStates][numberOfStates];
		
		for (int i = 0; i < numberOfStates; i++) {
			for (int j = 0; j < numberOfStates; j++) {
				String s = config.get("rate_" + (i+1) + "," + (j+1));
				if (s != null) {
					generatorMatrix[i][j] = Double.parseDouble(s);
				} else {
					generatorMatrix[i][j] = 0.0;
				}
			}
		}
		
		embeddedDTMC = new double[numberOfStates][numberOfStates];
		intensities = new double[numberOfStates];
		
		for (int i = 0; i < numberOfStates; i++) {
			double l = 0.0;
			
			// Fill the diagonal entries of the generator matrix with the
			// negative sum of the rates.
			for (int j = 0; j < numberOfStates; j++) {
				l += generatorMatrix[i][j];
			}
			generatorMatrix[i][i] = -l;
			
			// Compute the embedded Markov chain. This is used for choosing state transitions.
			for (int j = 0; j < numberOfStates; j++) {
				if (i != j) {
					embeddedDTMC[i][j] = - generatorMatrix[i][j] / generatorMatrix[i][i];
				} else {
					embeddedDTMC[i][j] = 0;
				}
			}
			
			// Fill the intensities vector. This is used for choosing sojourn times.
			intensities[i] = 1000 * (1 / generatorMatrix[i][i]);
		}
	}
	
	/**
	 * Configure the instance directly from a DTMC. The following parameters are expected
	 * in the configuration file:
	 * 
	 * Array of doubles   intensities -- Intensities (i.e. inverse rates, means in seconds)
	 * double             p_i,j       -- Probability for transition from i to j.
	 * 
	 * @param config
	 */
	
	private void configureFromDTMC(Hashtable<String, String> config) {
		// Read intensities vector
		// Read transition probabilities
		if (! Util.checkConfigRequired(this.getClass().getCanonicalName(), config, new String[] {"intensities"} )) {
			System.exit(-1);
		}
		
		intensities = new double[numberOfStates];
		String[] siv = config.get("intensities").split("\\s*,\\s*");
		if (siv.length != numberOfStates) {
			System.err.println(this.getClass().getCanonicalName() + ": Length of intensities vector does not match number of states.");
			System.exit(-1);
		}
		
		for (int i = 0; i < numberOfStates; i++) {
			intensities[i] = - Double.parseDouble(siv[i]) * 1000;
		}
		
		embeddedDTMC = new double[numberOfStates][numberOfStates];
		
		for (int i = 0; i < numberOfStates; i++) {
			for (int j = 0; j < numberOfStates; j++) {
				String s = config.get("p_" + (i+1) + "," + (j+1));
				if (s != null) {
					embeddedDTMC[i][j] = Double.parseDouble(s);
				} else {
					embeddedDTMC[i][j] = 0.0;
				}
				if (embeddedDTMC[i][j] < 0 || embeddedDTMC[i][j] > 1) {
					System.err.println(this.getClass().getCanonicalName() + ": p_" + i + "," + j + " not in [0, 1]");
					System.exit(-1);
					}
			}
		}
		
		
	}
	
	public double[] getInitialVector() {
		return Arrays.copyOf(initialVector, initialVector.length);
	}
	
	public void setInitialVector(double[] initialVector) {
		this.initialVector = Arrays.copyOf(initialVector, initialVector.length);
	}

}
