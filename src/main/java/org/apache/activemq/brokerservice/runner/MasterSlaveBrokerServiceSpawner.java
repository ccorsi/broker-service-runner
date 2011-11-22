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
public abstract class MasterSlaveBrokerServiceSpawner extends AbstractBrokerServiceSpawner {

	private int numberOfMasterSlavesPairs = 5;

	private int amqPort = 61616;
	
	public enum TYPE { MASTER, SLAVE }
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		AbstractBrokerServiceSpawner spawner = new MasterSlaveBrokerServiceSpawner() {

			private int currentPort;
			private int masterPort;

			@Override
			protected String getMasterTemplatePrefix() {
				return "master-activemq";
			}

			@Override
			protected String getSlaveTemplatePrefix() {
				return "slave-activemq";
			}

			@Override
			protected void populateProperties(TYPE type, Properties props, int id) {
				switch(type) {
				case MASTER:
					masterPort = currentPort++;
					props.setProperty("master.suffix.name", String.valueOf(id));
					props.setProperty("kahadb.dir", "master");
					props.setProperty("kahadb.prefix", String.valueOf(id));
					props.setProperty("master.hostname", "localhost");
					props.setProperty("master.port.number", String.valueOf(masterPort));
					break;
				case SLAVE:
					props.setProperty("slave.suffix.name", String.valueOf(id));
					props.setProperty("kahadb.dir", "slave");
					props.setProperty("kahadb.prefix", String.valueOf(id));
					props.setProperty("master.hostname", "localhost");
					props.setProperty("master.port.number", String.valueOf(masterPort));
					props.setProperty("slave.hostname", "localhost");
					props.setProperty("slave.port.number", String.valueOf(currentPort++));
					break;
				}
			}

			/* (non-Javadoc)
			 * @see org.apache.activemq.brokerservice.runner.AbstractBrokerServiceSpawner#preCreateSpawners()
			 */
			@Override
			protected void preCreateSpawners() {
				this.currentPort = getAmqPort();
			}
			
		};
		spawner.execute();

		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
		}

		spawner.stopBrokers();
		
	}

	/**
	 * @return the numberOfMasterSlavesPairs
	 */
	public int getNumberOfMasterSlavesPairs() {
		return numberOfMasterSlavesPairs;
	}

	/**
	 * @param numberOfMasterSlavesPairs the numberOfMasterSlavesPairs to set
	 */
	public void setNumberOfMasterSlavesPairs(int numberOfMasterSlavesPairs) {
		this.numberOfMasterSlavesPairs = numberOfMasterSlavesPairs;
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
	public void setAmqPort(int amqPort) {
		this.amqPort = amqPort;
	}

	/**
	 * 
	 */
	protected void createSpawners() {
		System.out.println("Using starting activemq port number: " + amqPort);
		Properties properties;
		
		spawners = new Spawner[2 * numberOfMasterSlavesPairs];
		for(int id = 0 ; id < numberOfMasterSlavesPairs ; id++) {
			// Start the master broker for the given id
			try {
				properties = new Properties();
				populateProperties(TYPE.MASTER, properties, id);
				String templatePrefix = this.getMasterTemplatePrefix();
				String configFileName = templatePrefix + "-" + id + ".xml";
				generateBrokerServiceConfigurationFiles(
						properties, templatePrefix + "-" + id + ".properties",
						templatePrefix + ".xml", configFileName);
				spawners[2 * id] = new Spawner(BrokerServiceProcess.class.getName(), "execute");
				List<String> jvmArgs = new LinkedList<String>();
                                addToJVMArgs(jvmArgs);
				jvmArgs.add("-Dbroker.config.file=xbean:" + configFileName);
				spawners[2 * id].setJVMArgs(jvmArgs);
				spawners[2 * id].setIdentifier("Master" + id);
				spawners[2 * id].spawnProcess();
			} catch (Exception e) {
				System.err.println("An exception was generated when spawning the embedded broker");
				e.printStackTrace();
			}
			// Start the slave broker for the given id
			try {
				/*
				 * Properties:
				 * 	- slave.suffix.name
				 * 	- kahadb.dir
				 * 	- kahadb.prefix
				 * 	- master.hostname
				 * 	- master.port.number
				 * 	- slave.hostname
				 * 	- slave.port.number
				 */
				Thread.sleep(5000); // sleep for 5 seconds to allow the spawner master broker to startup properly....
				String templatePrefix = this.getSlaveTemplatePrefix();
				String configFileName = templatePrefix + "-" + id + ".xml";
				properties = new Properties();
				populateProperties(TYPE.SLAVE, properties, id);
				generateBrokerServiceConfigurationFiles(
						properties, templatePrefix + "-" + id + ".properties",
						templatePrefix + ".xml", configFileName);
				spawners[2 * id + 1] = new Spawner(BrokerServiceProcess.class.getName(), "execute");
				List<String> jvmArgs = new LinkedList<String>();
                                addToJVMArgs(jvmArgs);
				jvmArgs.add("-Dbroker.config.file=xbean:" + configFileName);
				spawners[2 * id + 1].setJVMArgs(jvmArgs);
				spawners[2 * id + 1].setIdentifier("Slave" + id);
				spawners[2 * id + 1].spawnProcess();
			} catch(Exception e) {
				System.err.println("An exception was generated when spawning the embedded broker");
				e.printStackTrace();
			}
		}
	}

	protected Properties generateProperties(int id, String amqType,
			String kahadbPrefix, String masterHostname,
			String masterPortNumber, String slaveHostname,
			String slavePortNumber) {
		Properties props = new Properties();
		props.setProperty(amqType + ".suffix.name", String.valueOf(id));
		props.setProperty("kahadb.dir", amqType);
		props.setProperty("kahadb.prefix", kahadbPrefix);
		props.setProperty("master.hostname", masterHostname);
		props.setProperty("master.port.number", masterPortNumber);
		props.setProperty("slave.hostname", slaveHostname);
		props.setProperty("slave.port.number", slavePortNumber);
		return props;
	}

	protected abstract String getMasterTemplatePrefix();
	
	protected abstract String getSlaveTemplatePrefix();
	
	protected abstract void populateProperties(TYPE type, Properties pros, int id);
	
}
