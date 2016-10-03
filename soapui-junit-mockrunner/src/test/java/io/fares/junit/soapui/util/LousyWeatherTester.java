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
package io.fares.junit.soapui.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.IOUtils;

/**
 * 
 * Tests the weather mock service example from the testharness.
 * 
 * @author Niels Bertram
 *
 */
public class LousyWeatherTester {

	/**
	 * Endppoint as hard coded in the soapui mockservice
	 */
	private static final String DEFAULT_ENDPOINT = "http://localhost:8099/weather";

	public static URL getWeatherMockSoapUIProject() {
		URL projectFile = LousyWeatherTester.class
				.getResource("/soapui/TestSoapUIProject-soapui-project.xml");
		assert projectFile != null : "expect to find the soapui test resource";
		return projectFile;
	}

	public static String testWeatherMockService() throws MalformedURLException,
			IOException {
		return testWeatherMockService(DEFAULT_ENDPOINT);
	}

	public static String testWeatherMockService(String endpoint)
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
		return rs;

	}
}
