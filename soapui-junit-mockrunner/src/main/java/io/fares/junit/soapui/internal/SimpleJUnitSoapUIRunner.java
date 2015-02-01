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
package io.fares.junit.soapui.internal;

import io.fares.junit.soapui.SoapUIMock;
import io.fares.junit.soapui.MockRunnerTask;

import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.mock.WsdlMockService;
import com.eviware.soapui.model.mock.MockRunner;
import com.eviware.soapui.model.mock.MockService;

public class SimpleJUnitSoapUIRunner implements SoapUIMock {

	/**
	 * Need to keep track of the executing runner
	 */
	private MockRunner runner;

	@Override
	public void start(MockRunnerTask task) {

		try {

			ClassLoader ccl = getClass().getClassLoader();

			@SuppressWarnings("unchecked")
			Class<WsdlProject> wpc = (Class<WsdlProject>) ccl
					.loadClass("com.eviware.soapui.impl.wsdl.WsdlProject");

			WsdlProject project = wpc.newInstance();
			project.loadProject(task.getProjectFile());
			MockService mockService = project.getMockServiceByName(task
					.getMockServiceName());

			if (mockService instanceof WsdlMockService) {
				WsdlMockService wms = (WsdlMockService) mockService;
				wms.setHost(task.getMockHost());
				if (task.isMockPortSet()) {
					wms.setPort(task.getMockPort());
				}
			}
			mockService.setPath(task.getMockPath());
			runner = mockService.start();

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public void stop() {
		if (runner != null && runner.isRunning()) {
			runner.stop();
		}
	}

	@Override
	public boolean isRunning() {
		return runner != null && runner.isRunning();
	}

}
