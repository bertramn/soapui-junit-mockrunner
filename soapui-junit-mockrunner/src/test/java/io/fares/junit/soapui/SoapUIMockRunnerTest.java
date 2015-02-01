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

import io.fares.junit.soapui.SoapUIMockRunner;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import static org.junit.Assert.*;

import org.junit.ClassRule;
import org.junit.Test;
import org.apache.commons.io.IOUtils;

public class SoapUIMockRunnerTest {

	public static Logger LOG = Logger.getLogger(SoapUIMockRunnerTest.class
			.getName());

	@ClassRule
	public static SoapUIMockRunner runner = new SoapUIMockRunner()
			.simpleBinding()
			.withProjectPath("soapui/TestSoapUIProject-soapui-project.xml")
			.withMockServiceName("WeatherMockService")
			.withMockHost("localhost").withMockPort(8080)
			.withMockPath("/weather-change");

	@Test
	public void testMockRunner() throws Exception {
		assertTrue(runner.isRunning());
		String result = testMockService(runner.getMockEndpoint());
		assertNotNull(result);
	}

	@Test
	public void testMockRunnerAgain() throws Exception {
		assertTrue(runner.isRunning());
		String result = testMockService(runner.getMockEndpoint());
		assertNotNull(result);
	}

	public static String testMockService(String endpoint)
			throws MalformedURLException, IOException {

		HttpURLConnection con = (HttpURLConnection) new URL(endpoint)
				.openConnection();
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
		LOG.fine(rs);
		return rs;

	}

}
