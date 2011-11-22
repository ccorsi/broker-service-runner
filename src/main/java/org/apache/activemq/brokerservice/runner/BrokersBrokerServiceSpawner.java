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
public abstract class BrokersBrokerServiceSpawner extends AbstractBrokerServiceSpawner {

	private static final Logger logger = LoggerFactory.getLogger(BrokersBrokerServiceSpawner.class);
	
	private int numberOfBrokers = 5;
	private int amqPort = 61616;
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		AbstractBrokerServiceSpawner spawner = new BrokersBrokerServiceSpawner() {

			private int currentPort;

			@Override
			protected String getTemplatePrefix() {
				return "activemq";
			}

			@Override
			protected void populateProperties(Properties props, int id) {
				props.setProperty("activemq.suffix.name", String.valueOf(id));
				props.setProperty("kahadb.dir", "activemq");
				props.setProperty("kahadb.prefix", String.valueOf(id));
				props.setProperty("hostname", "localhost");
				props.setProperty("port.number", String.valueOf(currentPort++));
			}

			/* (non-Javadoc)
			 * @see org.apache.activemq.brokerservice.runner.AbstractBrokerServiceSpawner#preCreateSpawners()
			 */
			@Override
			protected void preCreateSpawners() {
				this.currentPort = this.getAmqPort();
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
	 * @param amqPort
	 */
	public void setAmqPort(int amqPort) {
		this.amqPort = amqPort;
	}
	
	/**
	 * @return the amqPort
	 */
	public int getAmqPort() {
		return amqPort;
	}

	public void setNumberOfBrokers(int numberOfBrokers) {
		this.numberOfBrokers = numberOfBrokers;
	}
	
	/* (non-Javadoc)
	 * @see org.apache.activemq.brokerservice.runner.AbstractBrokerServiceSpawner#createSpawners()
	 */
	@Override
	protected void createSpawners() {
		System.out.println("Using starting activemq port number: " + amqPort);
		Properties properties;
		
		spawners = new Spawner[numberOfBrokers];
		String prefix = getTemplatePrefix();
		for(int brokerId = 0 ; brokerId < numberOfBrokers ; brokerId++) {
			properties = new Properties();
			populateProperties(properties, brokerId);
			// Start the master broker for the given id
			try {
				String configFileName = prefix + "-" + brokerId + ".xml";
				generateBrokerServiceConfigurationFiles(
						properties, prefix + "-" + brokerId + ".properties",
						prefix + ".xml", configFileName);
				spawners[brokerId] = new Spawner(
						BrokerServiceProcess.class.getName(), "execute");
				List<String> jvmArgs = new LinkedList<String>();
                                addToJVMArgs(jvmArgs);
				jvmArgs.add("-Dbroker.config.file=xbean:" + configFileName);
				spawners[brokerId].setJVMArgs(jvmArgs);
				spawners[brokerId].setIdentifier("ActiveMQBroker" + brokerId);
				spawners[brokerId].spawnProcess();
			} catch (Exception e) {
				logger.debug("An exception was generated when spawning the embedded broker",e);
			}
		}
	}

	/**
	 * @return The prefix of the broker configuration file
	 */
	protected abstract String getTemplatePrefix();
	
	/**
	 * @param props
	 * @param id
	 */
	protected abstract void populateProperties(Properties props, int id);
	
	
}
