package io.fares.junit.soapui.outside.test;

import static org.junit.Assert.*;
import org.junit.ClassRule;
import org.junit.Test;

import io.fares.junit.soapui.SoapUIMockRunner;

public class SomeMockServiceTest {

	@ClassRule
	public static SoapUIMockRunner runner = new SoapUIMockRunner()
			.simpleBinding()
			.withProjectPath(
					"embedded-soapui/TestSoapUIProject-soapui-project.xml")
			.withMockServiceName("WeatherMockService")
			.withMockHost("localhost").withMockPort(8097)
			.withMockPath("/weather-change");

	@Test
	public void testMockRunner() throws Exception {
		assertTrue(runner.isRunning());
		System.out.println("do some testing against endpoint: "
				+ runner.getMockEndpoint());
	}

}