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

import java.io.IOException;

import org.slf4j.LoggerFactory;
import org.valhalla.tools.net.CommandExecutor;
import org.valhalla.tools.net.server.ServiceServer;

/**
 * @author Claudio Corsi
 *
 */
public class EnhancedBrokerServiceManager implements IBrokerServiceManager {

	private ServiceServer<EnhancedBrokerServiceManager, EnhancedBrokerServiceProcess> server;
	private StateEnum state = StateEnum.INITIALIZED;
	
	public EnhancedBrokerServiceManager() throws IOException {
		server = new ServiceServer<EnhancedBrokerServiceManager, EnhancedBrokerServiceProcess>() {

			@Override
			protected void serviceStarted() {
				state = StateEnum.STARTED;
			}

			@Override
			protected void serviceStopped() {
				state = StateEnum.STOPPED;
			}
		};
	}
	
	/* (non-Javadoc)
	 * @see org.apache.activemq.brokerservice.runner.IBrokerServiceManager#stopBroker()
	 */
	@Override
	public void stopBroker() {
		if (state  == StateEnum.STARTED) {
			// Send a stop message to the other process...
			server.enqueue(new CommandExecutor<EnhancedBrokerServiceProcess, EnhancedBrokerServiceManager>() {

				private static final long serialVersionUID = 1L;
				
				@Override
				public CommandExecutor<EnhancedBrokerServiceManager, EnhancedBrokerServiceProcess> apply(
						EnhancedBrokerServiceProcess process) {
					try {
						process.stopBroker();
					} catch (Exception e) {
						LoggerFactory.getLogger(this.getClass()).debug("An exception was raised", e);
					}
					return null;
				}
				
			});
		}
	}

	/**
	 * This will be called by the remote process and used to set the current state to STOPPED.
	 */
	public void brokerStopped() {
		this.state = StateEnum.STOPPED;
	}

}
