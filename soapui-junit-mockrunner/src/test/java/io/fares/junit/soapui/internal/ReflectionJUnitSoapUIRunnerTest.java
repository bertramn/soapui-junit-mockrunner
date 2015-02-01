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

import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import org.junit.Test;

import org.apache.commons.io.IOUtils;

import io.fares.junit.classloader.FilteringClassLoaderFactory;
import io.fares.junit.soapui.SoapUIMock;
import io.fares.junit.soapui.MockRunnerTask;
import io.fares.junit.soapui.SoapUIMockExecutor;

public class ReflectionJUnitSoapUIRunnerTest {

	public static Logger LOG = Logger
			.getLogger(ReflectionJUnitSoapUIRunnerTest.class.getName());

	// @Ignore
	@Test
	public void test() throws Exception {

		URL projectFile = getClass().getResource(
				"/soapui/TestSoapUIProject-soapui-project.xml");

		assertNotNull("expect to find the soapui test resource", projectFile);

		// plain system repo

		// hand it to the factory it will setup
		FilteringClassLoaderFactory clf = new FilteringClassLoaderFactory();

		// need to add this classes bundle to the jail for testing, because we
		// need to jail to also load the implementations of the
		// JUnitSoapUIRunners .. not sure how to do it in real world yet

		// create a class loader with the tests classloader as parent
		// this parent will be firewalled
		ClassLoader cl = clf.createClassLoader(getClass().getClassLoader(),
				SoapUIMock.class, MockRunnerTask.class, getClass());

		SoapUIMockExecutor executor = new SoapUIMockExecutor(cl,
				SoapUIMockExecutor.SIMPLE_IMPL);

		MockRunnerTask task = new MockRunnerTask().withProjectFile(projectFile)
				.withMockServiceName("WeatherMockService");

		// start the mock
		executor.start(task);

		// test it out

		assertTrue(executor.isRunning());

		HttpURLConnection con = (HttpURLConnection) new URL(
				"http://localhost:8099/weather").openConnection();
		con.setRequestMethod("POST");
		con.addRequestProperty("Accept", "application/soap+xml");
		con.addRequestProperty("Content-Type", "application/soap+xml");
		con.setDoOutput(true);
		con.getOutputStream()
				.write("<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\"><soap:Header/><soap:Body><GetWeather xmlns=\"http://www.webserviceX.NET\"><CityName>Brisbane</CityName></GetWeather></soap:Body></soap:Envelope>"
						.getBytes("UTF-8"));
		InputStream is = con.getInputStream();

		StringWriter writer = new StringWriter();
		IOUtils.copy(is, writer, "UTF-8");
		String rs = writer.toString();

		LOG.info(rs);

		// stop it
		executor.stop();

	}
}
