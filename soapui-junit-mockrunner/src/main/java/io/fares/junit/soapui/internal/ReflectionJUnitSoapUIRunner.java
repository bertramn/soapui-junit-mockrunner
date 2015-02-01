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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

import static org.unitils.util.ReflectionUtils.*;

public class ReflectionJUnitSoapUIRunner implements SoapUIMock {

	Object runner;

	private static Class<?> wsdlProjectClass;
	private static Class<?> mockServiceClass;
	private static Class<?> mockRunnerClass;

	private static Method loadProjectMethod;

	private static Method mockServiceStartMethod;
	private static Method mockServiceByNameMethod;

	private static Method mockRunnerIsRunningMethod;
	private static Method mockRunnerStopMethod;

	@Override
	public void start(MockRunnerTask task) {

		try {
			ClassLoader cl = Thread.currentThread().getContextClassLoader();

			wsdlProjectClass = cl
					.loadClass("com.eviware.soapui.impl.wsdl.WsdlProject");

			Object project = wsdlProjectClass.newInstance();

			// set project file
			invokeMethod(project, getLoadProjectMethod(), task.getProjectFile());

			// get mock service from WSDL project
			// create the mock service
			Object mockService = invokeMethod(project,
					getMockServiceByNameMethod(), task.getMockServiceName());

			if (mockService == null) {
				throw new IllegalArgumentException("MockService "
						+ task.getMockServiceName()
						+ " does not exist in project.");
			} else {

				mockServiceClass = mockService.getClass();
				mockServiceStartMethod = getMethod(mockServiceClass, "start",
						false);

				// create the mock runner by starting the mockservice
				runner = invokeMethod(mockService, mockServiceStartMethod);
				mockRunnerClass = runner.getClass();

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void stop() {
		if (runner != null && isRunning()) {

			try {
				invokeMethod(this.runner, getMockRunnerStopMethod());
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}

		}
	}

	public boolean isRunning() {

		if (runner != null) {
			try {
				Object answer = invokeMethod(this.runner,
						getMockRunnerIsRunningMethod());
				return Boolean.TRUE.equals(answer);
			} catch (InvocationTargetException e) {
				return false;
			}
		} else {
			return false;
		}

	}

	private Method getLoadProjectMethod() {
		if (wsdlProjectClass == null) {
			throw new IllegalStateException("wsdl project class not set");
		}
		if (loadProjectMethod == null) {
			loadProjectMethod = getMethod(wsdlProjectClass, "loadProject",
					false, URL.class);
		}
		return loadProjectMethod;
	}

	private Method getMockServiceByNameMethod() {
		if (wsdlProjectClass == null) {
			throw new IllegalStateException("wsdl project class not set");
		}
		if (mockServiceByNameMethod == null) {
			mockServiceByNameMethod = getMethod(wsdlProjectClass,
					"getMockServiceByName", false, String.class);
		}
		return mockServiceByNameMethod;
	}

	private Method getMockRunnerStopMethod() {

		if (mockRunnerClass == null) {
			throw new IllegalStateException("mockrunner class not set");
		}

		if (mockRunnerStopMethod == null) {
			mockRunnerStopMethod = getMethod(mockRunnerClass, "stop", false);
		}
		return mockRunnerStopMethod;
	}

	private Method getMockRunnerIsRunningMethod() {

		if (mockRunnerClass == null) {
			throw new IllegalStateException("mockrunner class not set");
		}

		if (mockRunnerIsRunningMethod == null) {
			mockRunnerIsRunningMethod = getMethod(mockRunnerClass, "isRunning",
					false);
		}
		return mockRunnerIsRunningMethod;
	}

}
