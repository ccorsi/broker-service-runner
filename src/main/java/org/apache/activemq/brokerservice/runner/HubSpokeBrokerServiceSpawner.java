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

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.valhalla.tools.process.Spawner;

/**
 * @author Claudio Corsi
 *
 */
public abstract class HubSpokeBrokerServiceSpawner extends AbstractBrokerServiceSpawner {

	private static final int defaultNumberOfHubs = 1;
	private static final int defaultNumberofSpokesPerHub = 5;
	
	private int numberOfHubs = defaultNumberOfHubs;
	private int numberOfSpokesPerHub = defaultNumberofSpokesPerHub;

	private int amqPort = 61616;
	
	public enum TYPE { HUB, SPOKE }
	
	/**
	 * @param amqDefaultPort the amqDefaultPort to set
	 */
	public void setAmqPort(int amqPort) {
		this.amqPort = amqPort;
	}

	/**
	 * @param numberOfSpokesPerHub the numberOfSpokesPerHub to set
	 */
	public void setNumberOfSpokesPerHub(int numberOfSpokesPerHub) {
		this.numberOfSpokesPerHub = numberOfSpokesPerHub;
	}

	/**
	 * @param numberOfHubs the numberOfHubs to set
	 */
	public void setNumberOfHubs(int numberOfHubs) {
		this.numberOfHubs = numberOfHubs;
	}

	/**
	 * @param numberOfSpokesPerHub the numberOfSpokesPerHub to set
	 * @return 
	 */
	protected int getNumberOfSpokesPerHub() {
		return this.numberOfSpokesPerHub;
	}

	/**
	 * @param numberOfHubs the numberOfHubs to set
	 * @return 
	 */
	protected int getNumberOfHubs() {
		return this.numberOfHubs;
	}

	/**
	 * 
	 */
	protected void createSpawners() {
		System.out.println("Using starting activemq port number: " + amqPort);
		
		spawners = new Spawner[numberOfSpokesPerHub * numberOfHubs + numberOfHubs];
		int spawnIdx = 0;
		Properties properties;
		for(int id = 0 ; id < numberOfHubs ; id++) {
			// Start the hub broker for the given id
			try {
				properties = new Properties();
				populateProperties(TYPE.HUB, properties, id);
				String hubTemplatePrefix = getHubTemplatePrefix();
				String hubPropertyFileName = hubTemplatePrefix + "-" + id + ".properties";
				String hubTemplateFileName = hubTemplatePrefix + ".xml";
				String hubConfigFileName = hubTemplatePrefix + "-" + id + ".xml";
				generateBrokerServiceConfigurationFiles(
						properties, hubPropertyFileName,
						hubTemplateFileName, hubConfigFileName);
				spawners[spawnIdx] = new Spawner(BrokerServiceProcess.class.getName(), "execute");
				List<String> jvmArgs = new LinkedList<String>();
				addToJVMArgs(jvmArgs);
				jvmArgs.add("-Dbroker.config.file=xbean:" + hubConfigFileName);
				spawners[spawnIdx].setJVMArgs(jvmArgs);
				spawners[spawnIdx].setIdentifier("Hub:" + id);
				spawners[spawnIdx].spawnProcess();
			} catch (Exception e) {
				System.err.println("An exception was generated when spawning the embedded broker");
				e.printStackTrace();
			}
			
			try {
				// sleep for 5 seconds to allow the spawner hub broker to startup properly....
				Thread.sleep(5000); 
			} catch (InterruptedException e) {
			}
			spawnIdx++;
			for (int spoke = 0; spoke < numberOfSpokesPerHub; spoke++) {
				// Start the spoke broker for the given id
				try {
					properties = new Properties();
					populateProperties(TYPE.SPOKE, properties, spoke);
					String spokeTemplatePrefix = getSpokeTemplatePrefix();
					String spokePropertyFileName = spokeTemplatePrefix + "-" + id + "-" + spoke
							+ ".properties";
					String spokeTemplateFileName = spokeTemplatePrefix
							+ ".xml";
					String spokeConfigFileName = spokeTemplatePrefix + "-" + id
							+ "-" + spoke + ".xml";
					generateBrokerServiceConfigurationFiles(properties,
							spokePropertyFileName, spokeTemplateFileName, spokeConfigFileName);
					spawners[spawnIdx] = new Spawner(
							BrokerServiceProcess.class.getName(), "execute");
					List<String> jvmArgs = new LinkedList<String>();
                                        addToJVMArgs(jvmArgs);
					jvmArgs.add("-Dbroker.config.file=xbean:" + spokeConfigFileName);
					spawners[spawnIdx].setJVMArgs(jvmArgs);
					spawners[spawnIdx].setIdentifier("Spoke:" + id + ":" + spoke);
					spawners[spawnIdx].spawnProcess();
				} catch (Exception e) {
					System.err
							.println("An exception was generated when spawning the embedded broker");
					e.printStackTrace();
				}
				spawnIdx++;
			}
		}
	}

	/**
	 * This method is called to setup the properties that will be included within the properties file
	 * that is used to populate the template file used to start the broker.
	 * 
	 * @param type
	 * @param properties
	 * @param idx
	 */
	protected abstract void populateProperties(TYPE type, Properties properties, int idx);

	/**
	 * This method will return the prefixed name of the hub template without the .xml suffix.
	 * It is assumed that the template is a file with the .xml extension.
	 * 
	 * @return
	 */
	protected abstract String getHubTemplatePrefix();
	
	/**
	 * This method will return the prefixed name of the spoke template without the .xml suffix.
	 * It is assumed that the template is a file with the .xml extension.
	 * 
	 * @return
	 */
	protected abstract String getSpokeTemplatePrefix();
	
	/**
	 * @return the amqPort
	 */
	protected int getAmqPort() {
		return amqPort;
	}
}
