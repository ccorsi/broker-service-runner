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

import org.valhalla.tools.process.Spawner;

/**
 * @author Claudio Corsi
 *
 */
public class HubSpokeBrokerServiceSpawnerOrg extends AbstractBrokerServiceSpawner {

	private static final int defaultNumberOfHubs = 1;
	private static final int defaultNumberofSpokesPerHub = 5;
	
	private int numberOfHubs = defaultNumberOfHubs;
	private int numberOfSpokesPerHub = defaultNumberofSpokesPerHub;

	private int amqPort = -1;
	private String amqDefaultPort;
	
	public enum TYPES { HUB, SPOKE }
	
	public HubSpokeBrokerServiceSpawnerOrg() {
		this("61616");
	}
	
	public HubSpokeBrokerServiceSpawnerOrg(String amqDefaultPort) {
		this.amqDefaultPort = amqDefaultPort;
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		AbstractBrokerServiceSpawner spawner = new HubSpokeBrokerServiceSpawnerOrg();
		spawner.execute();

		try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
		}

		spawner.stopBrokers();
		
	}

	/**
	 * @param amqDefaultPort the amqDefaultPort to set
	 */
	public void setAmqDefaultPort(String amqDefaultPort) {
		this.amqDefaultPort = amqDefaultPort;
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
	 * 
	 */
	protected void createSpawners() {
		int port;
		amqPort = Integer.parseInt(System.getProperty("amq.starting.port.number", amqDefaultPort));
		System.out.println("Using starting activemq port number: " + amqPort);
		
		port = amqPort;
		
		spawners = new Spawner[numberOfSpokesPerHub * numberOfHubs + numberOfHubs];
		int spawnIdx = 0;
		Properties properties;
		for(int id = 0 ; id < numberOfHubs ; id++) {
			String kahadbPrefix = String.valueOf(id);
			String hubHostname = "localhost";
			String hubPortNumber = String.valueOf(port++);
			String spokeHostname = "localhost";
			String spokePortNumber = hubPortNumber;
			// Start the hub broker for the given id
			try {
				properties = generateProperties(String.valueOf(id), "hub",
						kahadbPrefix, hubHostname, hubPortNumber, spokeHostname,
						spokePortNumber);
				generateBrokerServiceConfigurationFiles(
						properties, "hub-" + id + ".properties",
						"hub-activemq.xml", "hub-activemq-" + id + ".xml");
				spawners[spawnIdx] = new Spawner(BrokerServiceProcess.class.getName(), "execute");
				List<String> jvmArgs = new LinkedList<String>();
				jvmArgs.add("-Dbroker.config.file=xbean:hub-activemq-" + id + ".xml");
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
					/*
					 * Properties: 
					 * 	- spoke.suffix.name 
					 * 	- kahadb.dir 
					 * 	- kahadb.prefix 
					 * 	- hub.hostname
					 *  - hub.port.number
					 *  - spoke.hostname
					 *  - spoke.port.number
					 */
					spokePortNumber = String.valueOf(port++);
					kahadbPrefix = String.valueOf(id) + "-" + String.valueOf(spoke);
					properties = generateProperties(String.valueOf(id) + "-" + String.valueOf(spoke),
							"spoke", kahadbPrefix, hubHostname, hubPortNumber,
							spokeHostname, spokePortNumber);
					generateBrokerServiceConfigurationFiles(
							properties, "spoke-" + id + "-" + spoke + ".properties",
							"spoke-activemq.xml", "spoke-activemq-" + id + "-" + spoke + ".xml");
					spawners[spawnIdx] = new Spawner(
							BrokerServiceProcess.class.getName(), "execute");
					List<String> jvmArgs = new LinkedList<String>();
					jvmArgs.add("-Dbroker.config.file=xbean:spoke-activemq-" + id + "-" + spoke + ".xml");
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
	 * @param id
	 * @param amqType
	 * @param kahadbPrefix
	 * @param hubHostname
	 * @param hubPortNumber
	 * @param spokeHostname
	 * @param spokePortNumber
	 * @return
	 */
	protected Properties generateProperties(String id, String amqType,
			String kahadbPrefix, String hubHostname, String hubPortNumber,
			String spokeHostname, String spokePortNumber) {
		Properties props = new Properties();
		props.setProperty(amqType + ".suffix.name", id);
		props.setProperty("kahadb.dir", amqType);
		props.setProperty("kahadb.prefix", kahadbPrefix);
		props.setProperty("hub.hostname", hubHostname);
		props.setProperty("hub.port.number", hubPortNumber);
		props.setProperty("spoke.hostname", spokeHostname);
		props.setProperty("spoke.port.number", spokePortNumber);
		return props;
	}
}
