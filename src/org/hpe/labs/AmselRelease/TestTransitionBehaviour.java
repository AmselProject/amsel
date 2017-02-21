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

/**
 * 
 * This is a TransitionBehaviour class that just prints a string when its behave()
 * method is called.
 * 
 * @author Philipp Reinecke, Hewlett Packard Labs Bristol, philipp.reinecke@hpe.com or philipp.reinecke@fu-berlin.de
 *
 */

public class TestTransitionBehaviour implements TransitionBehaviour {

	@Override
	public void configure(Hashtable<String, String> config) {
		// TODO Auto-generated method stub

		System.out.println("Configuring transition behaviour.");
		
	}

	@Override
	public void behave() {
		// TODO Auto-generated method stub
		
		System.out.println("Transition occured!");

	}

}
