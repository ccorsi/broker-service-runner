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
import java.net.Socket;

/**
 * @author Claudio Corsi
 *
 */
public class BrokerServiceManager {
	
	private Socket socket;
	
	public BrokerServiceManager(Socket socket) {
		this.socket = socket;
	}

	public boolean waitUntilStarted() throws IOException {
		if (socket != null) {
			socket.getInputStream().read();
			return true;
		}
		return false;
	}
	
	public void stopBroker() throws IOException {
		if (this.socket != null) {
			this.socket.getOutputStream().write(1);
			if (Boolean.getBoolean("notifyIfStopped")) {
				// TODO:  I do not like this callback mechanism.  Find a better way....
				this.waitUntilStopped();
			}
			this.socket.close();
			this.socket = null;
		}
	}

	private void waitUntilStopped() throws IOException {
		if (this.socket != null) {
			this.socket.getInputStream().read();
		}
	}
}
