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

import java.util.logging.Logger;

import static org.junit.Assert.*;

import org.junit.Test;

import io.fares.classloader.AetherClasspathResolver;
import io.fares.classloader.ClasspathResolver;
import io.fares.classloader.FilteringClassLoaderFactory;
import static io.fares.junit.soapui.SoapUI.*;
import static io.fares.junit.soapui.util.LousyWeatherTester.*;
import io.fares.junit.soapui.SoapUIMock;
import io.fares.junit.soapui.SoapUIMockExecutor;
import io.fares.junit.soapui.MockRunnerTask;

public class ReflectionJUnitSoapUIRunnerTest {

	public static Logger LOG = Logger
			.getLogger(ReflectionJUnitSoapUIRunnerTest.class.getName());

	// @Ignore
	@Test
	public void test() throws Exception {

		ClasspathResolver resolver = new AetherClasspathResolver();

		// first need to configure the resolver with soapui dependency and repo
		resolver.addArtifact(newSoapUIArtifact());

		// it pays to add central as well cause sometimes soapui does not
		// contain all dependencies and we are not really reading the soapui pom
		resolver.addRemoteRepository(newSoapUIRepository(),
				newCentralRepository());

		// hand it to the factory it will setup
		FilteringClassLoaderFactory clf = new FilteringClassLoaderFactory(
				resolver);

		// add filters
		clf.addPassFilters(DEFAULT_PASSFILTER);
		clf.addBlockFilters(DEFAULT_BLOCKFILTER);

		// include this test (basedir.target.test-classes)
		clf.addIncludeClazzContainerURLs(getClass());

		// alsways need to add the container of this class (self) as we need to
		// get this through the filtering classloader else our code will fail
		// with class cast exception
		clf.addIncludeClazzContainerURLs(SoapUIMock.class, MockRunnerTask.class);

		MockRunnerTask task = new MockRunnerTask().withProjectFile(
				getWeatherMockSoapUIProject()).withMockServiceName(
				"WeatherMockService");

		// create a class loader with the tests classloader as parent
		// this parent will be firewalled
		clf.setParentClassLoader(getClass().getClassLoader());

		SoapUIMockExecutor executor = new SoapUIMockExecutor(clf,
				SoapUIMockExecutor.REFELCTION_IMPL);

		executor.start(task);
		assertTrue(executor.isRunning());
		String rs = testWeatherMockService();
		LOG.info(rs);
		executor.stop();

	}
}
