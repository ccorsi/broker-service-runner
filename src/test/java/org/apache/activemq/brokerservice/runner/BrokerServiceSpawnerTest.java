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
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Claudio Corsi
 *
 */
public class BrokerServiceSpawnerTest {

	private static final Logger logger = LoggerFactory.getLogger(BrokerServiceSpawnerTest.class);
	
	private String priorStartedNotificationSetting;
	private String priorStoppedNotificationSetting;
	
	@Before
	public void enableNotifications() {
		this.priorStartedNotificationSetting = System.getProperty("notifyIfStarted");
		System.setProperty("notifyIfStarted", "true");
		this.priorStoppedNotificationSetting = System.getProperty("notifyIfStopped");
		System.setProperty("notifyIfStopped", "true");
	}
	
	@After
	public void disableNotfications() {
		if (priorStoppedNotificationSetting != null)
			System.setProperty("notifyIfStopped", priorStoppedNotificationSetting);
		if (priorStartedNotificationSetting != null)
			System.setProperty("notifyIfStarted", priorStartedNotificationSetting);
	}
	
	/*
	 * Test method for {@link org.apache.activemq.brokerservice.runner.HubSpokeBrokerServiceSpawner#execute()}.
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	/*
	@Test
	public void testHubSpokeBrokerServiceSpawner() throws IOException, InterruptedException {
		AbstractBrokerServiceSpawner spawner = new HubSpokeBrokerServiceSpawner();
		spawner.execute();
		
		Thread.sleep(15000);
		logger.info("All hub/spoke brokers have been started"); // Thread.sleep(15000);
		
		spawner.stopBrokers();
		
	}
	*/
	
	@Test
	public void testBrokersBrokerServiceSpawner() throws IOException, InterruptedException {
		AbstractBrokerServiceSpawner spawner = new BrokersBrokerServiceSpawner() {

			private int currentPort;

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.apache.activemq.brokerservice.runner.AbstractBrokerServiceSpawner
			 * #preCreateSpawners()
			 */
			@Override
			protected void preCreateSpawners() {
				this.currentPort = this.getAmqPort();
			}

			@Override
			protected String getTemplatePrefix() {
				return "activemq";
			}

			@Override
			protected void populateProperties(Properties props, int id) {
				props.setProperty("activemq.suffix.name", String.valueOf(id));
				props.setProperty("amqdb.dir", "activemq");
				props.setProperty("amqdb.prefix", String.valueOf(id));
				props.setProperty("hostname", "localhost");
				props.setProperty("port.number", String.valueOf(currentPort++));
			}

		};
		spawner.execute();
		
		Thread.sleep(15000);
		logger.info("All brokers have been started"); // Thread.sleep(15000);
		
		spawner.stopBrokers();
		
	}

	@Test
	public void testMasterSlaveBrokerServiceSpawner() throws IOException, InterruptedException {
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
					props.setProperty("amqdb.dir", "master");
					props.setProperty("amqdb.prefix", String.valueOf(id));
					props.setProperty("master.hostname", "localhost");
					props.setProperty("master.port.number", String.valueOf(masterPort));
					break;
				case SLAVE:
					props.setProperty("slave.suffix.name", String.valueOf(id));
					props.setProperty("amqdb.dir", "slave");
					props.setProperty("amqdb.prefix", String.valueOf(id));
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
			
			{
				setNumberOfMasterSlavesPairs(2);
			}
		};
		spawner.execute();
		
		logger.info("All master/slave brokers have been started"); 
		
		logger.info("Let us wait 5 seconds to allow the network connections to connect");
		
		Thread.sleep(5000);
		
		Thread.sleep(15000);
		spawner.stopBrokers();
		
	}

	@Test
	public void testNetworkBrokersBrokerServiceSpawner() throws IOException, InterruptedException {
		AbstractBrokerServiceSpawner spawner = new NetworkBrokersBrokerServiceSpawner(){

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
					props.setProperty("amqdb.dir", "main");
					props.setProperty("amqdb.prefix", mainId + "-0");
					props.setProperty("hostname", "localhost");
					props.setProperty("port.number", String.valueOf(mainPort));
					break;
				case NETWORKED:
					props.setProperty("network.suffix.name", String.valueOf(id));
					props.setProperty("amqdb.dir", "network");
					props.setProperty("amqdb.prefix", mainId + "-" + id);
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
		
		Thread.sleep(15000);
		logger.info("All network brokers setup has been started"); // Thread.sleep(15000);
		
		spawner.stopBrokers();
		
	}

	@Test
	public void testHubSpokeBrokerServiceSpawnerEx() throws IOException, InterruptedException {
		AbstractBrokerServiceSpawner spawner = new HubSpokeBrokerServiceSpawner() {

			private int hubId;
			private int currentPort = -1;
			private int hubPort;
			private int spokePort;
			
			/* (non-Javadoc)
			 * @see org.apache.activemq.brokerservice.runner.AbstractBrokerServiceSpawner#preCreateSpawners()
			 */
			@Override
			protected void preCreateSpawners() {
				currentPort = this.getAmqPort();
			}

			@Override
			protected void populateProperties(TYPE type,
					Properties properties, int idx) {
				switch(type) {
				case HUB:
					populateHubProperties(properties, idx);
					break;
				case SPOKE:
					populateSpokeProperties(properties, idx);
					break;
				}
			}

			private void populateSpokeProperties(Properties props, int id) {
				spokePort = currentPort++;
				props.setProperty("spoke.suffix.name", String.valueOf(hubId) + "-" + String.valueOf(id));
				props.setProperty("amqdb.dir", "spoke");
				props.setProperty("amqdb.prefix", String.valueOf(id));
				props.setProperty("hub.hostname", "localhost");
				props.setProperty("hub.port.number", String.valueOf(hubPort));
				props.setProperty("spoke.hostname", "localhost");
				props.setProperty("spoke.port.number", String.valueOf(spokePort));
			}

			private void populateHubProperties(Properties props, int id) {
				hubId = id;
				hubPort = currentPort++;
				props.setProperty("hub.suffix.name", String.valueOf(id));
				props.setProperty("amqdb.dir", "hub");
				props.setProperty("amqdb.prefix", String.valueOf(id));
				props.setProperty("hub.hostname", "localhost");
				props.setProperty("hub.port.number", String.valueOf(hubPort));
			}

			@Override
			protected String getHubTemplatePrefix() {
				return "hub-activemq";
			}

			@Override
			protected String getSpokeTemplatePrefix() {
				return "spoke-activemq";
			}
			
		};
		
		spawner.execute();
		
		Thread.sleep(15000);
		logger.info("All hub/spoke brokers have been started using the extended hub/spoke version"); // Thread.sleep(15000);
		
		spawner.stopBrokers();
		
	}

}
