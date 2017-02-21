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
 * StateBehaviour describes the behaviour that occurs when a state-space
 * model is in a state.
 *
 * Every implementation should expect that startBehaviour() is used to
 * 
 * (a) initially start behaviour (i.e. no behaviour should be observable
 *     startBehaviour() has been called),
 * (b) resume behaviour after it has been paused with stopBehaviour().
 * 
 * Every implementation also must expect the use of stopBehaviour() for
 * 
 * (a) pausing behaviour generation (i.e. observable behaviour should cease after
 *     stopBehaviour() has been called, but the process must be resumable by
 *     startBehaviour()),
 * (b) permanently stopping behaviour generation (which will be signalled by
 *     Util.exitAllThreads == true).
 * 
 * @author Philipp Reinecke, Hewlett Packard Labs Bristol, philipp.reinecke@hpe.com or philipp.reinecke@fu-berlin.de
 *
 */

import java.util.Hashtable;

public interface StateBehaviour {

	public void configure(Hashtable<String, String> config) throws Exception;

	public void startBehaviour();
	public void stopBehaviour();

}
