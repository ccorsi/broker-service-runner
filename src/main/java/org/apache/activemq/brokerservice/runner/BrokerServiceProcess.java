/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.brokerservice.runner;

import java.net.Socket;
import java.net.URI;

import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;

/**
 * @author Claudio Corsi
 *
 */
public class BrokerServiceProcess {

	public void execute() throws Exception {
		// This is the main method that will be used to start the embedded brokers.
		// It will expect to find the broker.config.file property to be set.
		String brokerConfigFile = System.getProperty("broker.config.file");
		if (brokerConfigFile == null) {
			// Raise an exception since no activemq configuration file was passed.
			throw new RuntimeException("No activemq configuration file was passed");
		}
		
		String port = System.getProperty("broker.service.port.number");
		if (port == null) {
			// Raise an exception since no port was passed by the spawned process to be
			// able to stop the started broker.
			throw new RuntimeException("No port number was passed by the spawning process");
		}
		
		Socket socket = new Socket("localhost", Integer.parseInt(port));
		
		System.out.println("STARTING BROKER USING " + brokerConfigFile + " configuration file.");
		// Load and start the broker using the passed activemq configuration file.
		BrokerService broker = BrokerFactory.createBroker(new URI(brokerConfigFile), true);
		// broker.waitUntilStarted(); FIXME: need to find another way to determine if broker started.
		
		if (Boolean.getBoolean("notifyIfStarted")) {
			// FIXME: Provide a better mechanism to inform parent process that we've started.
			socket.getOutputStream().write(2);
		}
		
		try {
			System.out.println("WAITING FOR CALLING PROCESS TO STOP THE BROKER");
			// FIXME: Provide a better mechanism to receive stop command from parent process.
			System.out.println("RECEIVED THE FOLLOWING INTEGER VALUE: " + socket.getInputStream().read());
		} finally {
			System.out.println("STOPPING BROKER");
			// Wait until the broker needs to be stopped.
			broker.stop();
			if (Boolean.getBoolean("notifyIfStopped")) {
				broker.waitUntilStopped();
				// FIXME: Provide a better mechanism to inform parent process that we've stopped
				socket.getOutputStream().write(3);
			}
		}
	}
}
