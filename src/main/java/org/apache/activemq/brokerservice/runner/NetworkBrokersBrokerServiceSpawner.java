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
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.valhalla.tools.process.Spawner;

/**
 * @author Claudio Corsi
 *
 */
public abstract class NetworkBrokersBrokerServiceSpawner extends AbstractBrokerServiceSpawner {

	private static final Logger logger = LoggerFactory.getLogger(NetworkBrokersBrokerServiceSpawner.class);
	
	private int numberOfNetworkBrokers = 5;
	private int numberOfNetworks = 3;

	private int amqPort = 61616;
	
	/**
	 * @author Claudio Corsi
	 *
	 */
	public enum TYPE { MAIN, NETWORKED }
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		AbstractBrokerServiceSpawner spawner = new NetworkBrokersBrokerServiceSpawner() {

			private int mainPort;
			private int currentPort;
			private int mainId;

			@Override
			protected String getMainTemplatePrefix() {
				return "main-activemq";
			}

			@Override
			protected String getNetworkedTemplatePrefix() {
				return "network-activemq";
			}

			@Override
			protected void populateProperties(TYPE type, Properties props,
					int id) {
				switch(type) {
				case MAIN:
					mainPort = currentPort++;
					mainId = id;
					props.setProperty("main.suffix.name", String.valueOf(id));
					props.setProperty("kahadb.dir", "main");
					props.setProperty("kahadb.prefix", mainId + "-0");
					props.setProperty("hostname", "localhost");
					props.setProperty("port.number", String.valueOf(mainPort));
					break;
				case NETWORKED:
					props.setProperty("network.suffix.name", String.valueOf(id));
					props.setProperty("kahadb.dir", "network");
					props.setProperty("kahadb.prefix", mainId + "-" + id);
					props.setProperty("hostname", "localhost");
					props.setProperty("port.number", String.valueOf(currentPort++));
					props.setProperty("network.hostname", "localhost");
					props.setProperty("network.port.number", String.valueOf(mainPort));
					break;
				}
			}

			/* (non-Javadoc)
			 * @see org.apache.activemq.brokerservice.runner.AbstractBrokerServiceSpawner#preCreateSpawners()
			 */
			@Override
			protected void preCreateSpawners() {
				currentPort = this.getAmqPort();
			}
			
		};
		spawner.execute();

		try {
			Thread.sleep(15000);
		} catch (InterruptedException e) {
		}

		spawner.stopBrokers();
		
	}

	/**
	 * @return the amqPort
	 */
	public int getAmqPort() {
		return amqPort;
	}

	/**
	 * @param amqPort the amqPort to set
	 */
	public void setAmqDefaultPort(int amqPort) {
		this.amqPort = amqPort;
	}

	/**
	 * @param numberOfNetworks the numberOfNetworks to set
	 */
	public void setNumberOfNetworks(int numberOfNetworks) {
		this.numberOfNetworks = numberOfNetworks;
	}

	/**
	 * @param numberOfNetworkBrokers the numberOfNetworkBrokers to set
	 */
	public void setNumberOfNetworkBrokers(int numberOfNetworkBrokers) {
		this.numberOfNetworkBrokers = numberOfNetworkBrokers;
	}

	protected abstract String getMainTemplatePrefix();
	
	protected abstract String getNetworkedTemplatePrefix();
	
	protected abstract void populateProperties(TYPE type, Properties props, int id);
	
	/**
	 * 
	 */
	protected void createSpawners() {
		logger.info("Using starting activemq port number: " + amqPort);
		Properties properties;
		
		spawners = new Spawner[numberOfNetworkBrokers * numberOfNetworks];
		int idx = 0;
		for(int net = 0 ; net < numberOfNetworks ; net++) {
			properties = new Properties();
			populateProperties(TYPE.MAIN, properties, net);
			// Start the master broker for the given id
			try {
				String mainTemplatePrefix = getMainTemplatePrefix();
				String mainConfigFileName = "main-activemq-" + net + "-0" + ".xml";
				generateBrokerServiceConfigurationFiles(
						properties, mainTemplatePrefix + "-" + net + "-0" + ".properties",
						mainTemplatePrefix + ".xml", mainConfigFileName );
				spawners[idx] = new Spawner(
						BrokerServiceProcess.class.getName(), "execute");
				List<String> jvmArgs = new LinkedList<String>();
                                addToJVMArgs(jvmArgs);
				jvmArgs.add("-Dbroker.config.file=xbean:" + mainConfigFileName);
				spawners[idx].setJVMArgs(jvmArgs);
				spawners[idx].setIdentifier("MainBroker" + net + "-0");
				spawners[idx].spawnProcess();
			} catch (Exception e) {
				System.err
						.println("An exception was generated when spawning the embedded broker");
				e.printStackTrace();
			}
			idx++;
			for(int id = 1 ; id < numberOfNetworkBrokers ; id++) {
				properties = new Properties();
				this.populateProperties(TYPE.NETWORKED, properties, id);
				// Start the master broker for the given id
				try {
					String networkedTemplatePrefix = getNetworkedTemplatePrefix();
					String networkedConfigFileName = "network-activemq-" + net + "-" + id + ".xml";
					generateBrokerServiceConfigurationFiles(properties, networkedTemplatePrefix + "-"
							+ net + "-" + id + ".properties", networkedTemplatePrefix + ".xml",
							networkedConfigFileName);
					spawners[idx] = new Spawner(BrokerServiceProcess.class.getName(), "execute");
					List<String> jvmArgs = new LinkedList<String>();
                                        addToJVMArgs(jvmArgs);
					jvmArgs.add("-Dbroker.config.file=xbean:" + networkedConfigFileName);
					spawners[idx].setJVMArgs(jvmArgs);
					spawners[idx].setIdentifier("NetworkBroker" + net + "-" + id);
					spawners[idx].spawnProcess();
				} catch (Exception e) {
					System.err.println("An exception was generated when spawning the embedded broker");
					e.printStackTrace();
				}
				idx++;
			}
		}
	}

	/**
	 * @param id
	 * @param amqType
	 * @param kahadbPrefix
	 * @param hostname
	 * @param portNumber
	 * @param networkHostname
	 * @param networkPortNumber
	 * @return
	 */
	protected Properties generateProperties(String id, String amqType, String kahadbPrefix, String hostname, String portNumber,
			String networkHostname, String networkPortNumber) {
		Properties props = new Properties();
		props.setProperty(amqType + ".suffix.name", id);
		props.setProperty("kahadb.dir", amqType);
		props.setProperty("kahadb.prefix", kahadbPrefix);
		props.setProperty("hostname", hostname);
		props.setProperty("port.number", portNumber);
		props.setProperty("network.hostname", networkHostname);
		props.setProperty("network.port.number", networkPortNumber);
		return props;
	}

}
