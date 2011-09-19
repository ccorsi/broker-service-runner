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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.valhalla.tools.process.Spawner;

/**
 * @author Claudio Corsi
 *
 */
public abstract class AbstractBrokerServiceSpawner {

	private static final Logger logger = LoggerFactory.getLogger(AbstractBrokerServiceSpawner.class);
	
	private Collection<BrokerServiceManager> managers = new LinkedList<BrokerServiceManager>();
	
	private int startedBrokers = 0;
	
	protected Spawner spawners[];

	/**
	 * @param properties
	 * @param propertyFileName
	 * @param activemqTemplate
	 * @param activemqConfigFileName
	 * @throws IOException
	 */
	protected final void generateBrokerServiceConfigurationFiles(
			Properties properties, String propertyFileName,
			String activemqTemplate, String activemqConfigFileName)
			throws IOException {
		File propFile = new File(propertyFileName);
		if (propFile.exists()) {
			propFile.delete();
		}
		PrintWriter writer = new PrintWriter(propFile);
		properties.store(writer, "Properties used by the "
				+ activemqConfigFileName + " activemq configuration file");
		InputStream is = ClassLoader
				.getSystemResourceAsStream(activemqTemplate);
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(
				activemqConfigFileName)));
		String line;
		while ((line = br.readLine()) != null) {
			pw.println(line.replace("@@PROPERTIESFILE@@", propFile.toURI()
					.toString()));
		}
		pw.close();
		br.close();
	}

	/**
	 * This is the main method that will manage the spawned broker services.
	 * 
	 * @throws IOException 
	 * 
	 */
	public final void execute() throws IOException {
		ServerSocket server = new ServerSocket(0);
		int port = server.getLocalPort();
		System.setProperty("broker.service.port.number", String.valueOf(port));
		new Thread(new Runnable() {
			private ServerSocket server;
			
			public Runnable setServerSocket(ServerSocket server) {
				this.server = server;
				return this;
			}
	
			@Override
			public void run() {
				do {
					try {
						Socket socket = server.accept();
						BrokerServiceManager manager = new BrokerServiceManager(socket);
						AbstractBrokerServiceSpawner.this.managers.add(manager);
						if (Boolean.getBoolean("notifyIfStarted")) {
							// Spawn a thread that will read the integer and increment started count...
							new Thread(new Runnable() {
								private BrokerServiceManager manager;

								public void run() {
									try {
										manager.waitUntilStarted();
									} catch (IOException e) {
										logger.info("Received an exception while waiting to be informed that the broker was started",e);
									} finally {
										synchronized(AbstractBrokerServiceSpawner.this) {
											// Increment the started count notify waiting parent thread
											AbstractBrokerServiceSpawner.this.startedBrokers++;
											AbstractBrokerServiceSpawner.this.notify();
										}
									}
								}
								
								public Runnable setManager(BrokerServiceManager manager) {
									this.manager = manager;
									return this;
								}
							}.setManager(manager)) {
								{
									this.setDaemon(true);
									this.start();
								}
							};
						}
					} catch (IOException e) {
						// Something happened but let us not worry about this just yet.
					}
				} while(stillActive());
			}
	
			private boolean stillActive() {
				for (Spawner spawner : AbstractBrokerServiceSpawner.this.spawners) {
					// FIXME: Add exited processes to a separate collection to
					// combat possible boundary conditions for wait until
					// started/stop cases
					if (!spawner.isProcessExited())
						return true;
				}
				return false;
			}
		}.setServerSocket(server))
		{
			{
				setDaemon(true);
				start();
			}
		};
		
		preCreateSpawners();
		createSpawners();
		postCreateSpawners();
		
		if (Boolean.getBoolean("notifyIfStarted")) {
			while (true) { // FIXME: We need a better way to combat unpredictable situations...like broker not starting...
				// Wait to be notified that a broker was started...
				synchronized (this) {
					try {
						wait();
					} catch (InterruptedException e) {
						logger.debug(
								"Received an exception while waiting to find out if all of the brokers have been started",
								e);
					} finally {
						if (this.startedBrokers >= this.spawners.length) {
							// We are done return...
							return;
						}
					}
				}
			}
		}
	}

	/**
	 * This is called prior to calling the createSpawners method.
	 */
	protected void postCreateSpawners() {
	}

	/**
	 * This is called after calling the createSpawners method.
	 */
	protected void preCreateSpawners() {
	}

	/**
	 * This method is implemented that is used to create the spawner instances that will be spawned and that the
	 * base class will manage is life cycle.
	 * 
	 * The overrided method has to use the spawner array
	 */
	protected abstract void createSpawners();

	/**
	 * @throws IOException
	 */
	public final void stopBrokers() throws IOException {
		for( BrokerServiceManager manager : managers) {
			manager.stopBroker();
		}
	}

}
