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

import java.net.URI;

import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;
import org.valhalla.tools.net.CommandExecutor;
import org.valhalla.tools.net.client.ServiceClient;
import org.valhalla.tools.net.server.ServiceServer;

/**
 * @author Claudio Corsi
 *
 */
public class EnhancedBrokerServiceProcess {
	
	private BrokerService broker;
	private ServiceClient<EnhancedBrokerServiceProcess, EnhancedBrokerServiceManager> client;

	public void execute() throws Exception {
		client = new ServiceClient<EnhancedBrokerServiceProcess, EnhancedBrokerServiceManager>() {

			@Override
			public void startedServiceManager() {
				// TODO Auto-generated method stub
				
			}
			
		};
		client.enqueue(new CommandExecutor<EnhancedBrokerServiceManager, EnhancedBrokerServiceProcess>() {

			private static final long serialVersionUID = 1L;

			@Override
			public CommandExecutor<EnhancedBrokerServiceProcess, EnhancedBrokerServiceManager> apply(
					EnhancedBrokerServiceManager destination) {
				// TODO Auto-generated method stub
				return null;
			}

		});
		// This is the main method that will be used to start the embedded brokers.
		// It will expect to find the broker.config.file property to be set.
		String brokerConfigFile = System.getProperty("broker.config.file");
		if (brokerConfigFile == null) {
			// Raise an exception since no activemq configuration file was passed.
			throw new RuntimeException("No activemq configuration file was passed");
		}
		
		System.out.println("STARTING BROKER USING " + brokerConfigFile + " configuration file.");
		broker = BrokerFactory.createBroker(new URI(brokerConfigFile), true);
		broker.waitUntilStarted();
	}

	public void stopBroker() throws Exception {
		System.out.println("STOPPING BROKER...");
		broker.stop();
		System.out.println("WAITING FOR BROKER TO COMPLETELY STOP");
		broker.waitUntilStopped();
		System.out.println("BROKER HAS STOPPED");
		client.enqueue(new CommandExecutor<EnhancedBrokerServiceManager, EnhancedBrokerServiceProcess>() {

			private static final long serialVersionUID = 1L;

			@Override
			public CommandExecutor<EnhancedBrokerServiceProcess, EnhancedBrokerServiceManager> apply(
					EnhancedBrokerServiceManager manager) {
				manager.brokerStopped();
				return null;
			}
		});
	}
}
