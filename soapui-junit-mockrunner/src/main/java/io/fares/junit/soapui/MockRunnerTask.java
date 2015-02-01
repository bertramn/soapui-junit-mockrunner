/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package io.fares.junit.soapui;

import java.net.URL;

public class MockRunnerTask {

	private URL projectFile;

	private String mockServiceName;

	private String mockHost;

	private int mockPort = -1;

	private String mockPath;

	private boolean securePort = false;

	public URL getProjectFile() {
		return projectFile;
	}

	public void setProjectFile(URL projectFile) {
		this.projectFile = projectFile;
	}

	public String getMockServiceName() {
		return mockServiceName;
	}

	public void setMockServiceName(String mockServiceName) {
		this.mockServiceName = mockServiceName;
	}

	public String getMockHost() {
		return (mockHost == null ? "localhost" : mockHost);
	}

	public void setMockHost(String host) {
		this.mockHost = host;
	}

	public boolean isSecurePort() {
		return this.securePort;
	}

	public void setSecurePort(boolean securePort) {
		this.securePort = securePort;
	}

	public int getMockPort() {
		return mockPort;
	}

	public boolean isMockPortSet() {
		return mockPort != -1;
	}

	public void setMockPort(int port) {
		this.mockPort = port;
	}

	public String getMockPath() {

		if (mockPath == null) {
			return "/";
		} else if (mockPath.startsWith("/")) {
			return mockPath;
		} else {
			return "/" + mockPath;
		}

	}

	public void setMockPath(String path) {
		this.mockPath = path;
	}

	public MockRunnerTask withProjectFile(URL file) {
		setProjectFile(file);
		return this;
	}

	public MockRunnerTask withMockServiceName(String name) {
		setMockServiceName(name);
		return this;
	}

	public MockRunnerTask withMockHost(String host) {
		setMockHost(host);
		return this;
	}

	public MockRunnerTask withMockPort(int port) {
		setMockPort(port);
		return this;
	}

	public MockRunnerTask withMockPath(String path) {
		setMockPath(path);
		return this;
	}

	public MockRunnerTask securePort() {
		setSecurePort(true);
		return this;
	}

	public String getQualifiedMockHost() {

		StringBuilder sb = new StringBuilder();

		// secure protocol
		sb.append("http");
		if (securePort) {
			sb.append('s');
		}
		sb.append("://");

		// default to localhost if not specified
		sb.append(getMockHost());

		// append port if set
		if (mockPort != -1 && !securePort) {
			sb.append(':');
			sb.append(mockPort);
		} else if (mockPort == -1 && securePort) {
			sb.append(':');
			sb.append(8443);
		}

		return sb.toString();

	}

	public String getMockEndpoint() {
		return getQualifiedMockHost() + getMockPath();
	}

}
