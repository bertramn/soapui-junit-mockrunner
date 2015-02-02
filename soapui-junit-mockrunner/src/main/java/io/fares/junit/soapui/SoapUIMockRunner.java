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

import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.util.repository.AuthenticationBuilder;

import static io.fares.junit.soapui.SoapUI.*;
import io.fares.classloader.AetherClasspathResolver;
import io.fares.classloader.ClasspathResolver;
import io.fares.classloader.FilteringClassLoaderFactory;

public class SoapUIMockRunner implements TestRule {

	// the test case in progress
	private Statement base;

	// used to run new thread in the jailed classloader
	private String implName = SoapUIMockExecutor.SIMPLE_IMPL;

	private String soapuiVersion;

	// used to build the jar dependencies of the soapui runtime
	ClasspathResolver resolver = new AetherClasspathResolver();

	// used to setup the classloader jail
	List<String> passFilters;
	List<String> blockFilters;
	List<Class<?>> includeClazzContainerURLs;

	// used to flag to include the unit test jar or file based classpath to the
	// jail class loader (e.g. to run a soapui wsdl from within the jail)
	private boolean includeUnitTestLocation = false;

	// used to control the startup and teardown of the soapui mock
	private MockRunnerTask task = new MockRunnerTask();

	/**
	 * Need to keep track of the executing runner
	 */
	private SoapUIMockExecutor runner;

	/**
	 * Creates a basic configured {@link SoapUIMockRunner}
	 */
	public SoapUIMockRunner() {
		this(DEFAULT_PASSFILTER, DEFAULT_BLOCKFILTER);
	}

	/**
	 * If you really must, just make sure your filters allow the SoapUI to find
	 * all its dependencies above the filtering class loader, else class cast
	 * nightmare.
	 * 
	 * @param passFilters
	 * @param blockFilters
	 * 
	 */
	public SoapUIMockRunner(String[] passFilters, String[] blockFilters) {
		addPassFilters(passFilters);
		addBlockFilters(blockFilters);
	}

	public Statement apply(Statement base, Description description) {
		this.base = base;
		return statement(base);
	}

	private Statement statement(final Statement base) {

		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				before();
				try {
					base.evaluate();
				} finally {
					after();
				}
			}
		};
	}

	protected void before() throws Throwable {

		if (task.getProjectFile() == null) {
			throw new RuntimeException(
					"a project resource location must be provided to the rule");
		}

		// first need to configure the resolver with soapui dependency and repo
		resolver.addArtifact(newSoapUIArtifact(soapuiVersion));

		// it pays to add central as well cause sometimes soapui does not
		// contain all dependencies and we are not really reading the soapui pom
		resolver.addRemoteRepository(newSoapUIRepository(),
				newCentralRepository());

		// then we need to create the filtering classloader
		FilteringClassLoaderFactory clf = new FilteringClassLoaderFactory(
				resolver);

		// add all filter rules the filtering classloader has to abide by
		clf.addPassFilters(passFilters);
		clf.addBlockFilters(blockFilters);

		// if requested, the location of the unit test will be added to the
		// classpath that is visible to the soapui itself (e.g. one can add any
		// extensions here)
		if (includeUnitTestLocation) {
			clf.addIncludeClazzContainerURLs(base.getClass());
		}

		// alsways need to add the container of this class (self) as we need to
		// get this through the filtering classloader else our code will fail
		// with class cast exception
		clf.addIncludeClazzContainerURLs(SoapUIMock.class, MockRunnerTask.class);

		// add classloader of the base test as parent, thats obviously the
		// context class loader here
		clf.setParentClassLoader(base.getClass().getClassLoader());

		// lets do this
		runner = new SoapUIMockExecutor(clf, implName);
		runner.start(task);

	}

	protected void after() {
		if (runner != null && runner.isRunning()) {
			runner.stop();
		}
	}

	public SoapUIMockRunner withMockServiceName(String name) {
		task.setMockServiceName(name);
		return this;
	}

	public SoapUIMockRunner withProjectFile(File file) {

		if (file == null) {
			throw new RuntimeException("File must not be null");
		} else if (!file.exists()) {
			throw new RuntimeException("File " + file.getAbsolutePath()
					+ " does not exist");
		} else if (!file.isFile()) {
			throw new RuntimeException("FileName " + file.getAbsolutePath()
					+ " is not a file");
		}

		try {
			task.setProjectFile(file.toURI().toURL());
		} catch (MalformedURLException e) {
			throw new RuntimeException("Cannot parse project file "
					+ file.getAbsolutePath(), e);
		}
		return this;
	}

	public SoapUIMockRunner withProjectFileName(String fileName) {
		return withProjectFile(new File(fileName));
	}

	/**
	 * set the project path of the SoapUI project
	 * 
	 * @param resourcePath
	 *            the resource's classpath
	 * @return this rule
	 */
	public SoapUIMockRunner withProjectPath(String resourcePath) {
		// try the local classloader
		URL url = getClass().getClassLoader().getResource(resourcePath);
		if (url == null) {
			throw new RuntimeException(
					"project cannot be loaded from resource path "
							+ resourcePath);
		}

		task.setProjectFile(url);
		return this;
	}

	public SoapUIMockRunner withProjectPath(URL resource) {
		task.setProjectFile(resource);
		return this;
	}

	public SoapUIMockRunner securePort() {
		task.securePort();
		return this;
	}

	public SoapUIMockRunner withMockHost(String host) {
		task.setMockHost(host);
		return this;
	}

	public SoapUIMockRunner withMockPort(int port) {
		task.setMockPort(port);
		return this;
	}

	public SoapUIMockRunner withMockPath(String path) {
		task.setMockPath(path);
		return this;
	}

	public SoapUIMockRunner simpleBinding() {
		this.implName = SoapUIMockExecutor.SIMPLE_IMPL;
		return this;
	}

	public SoapUIMockRunner reflectionBinding() {
		this.implName = SoapUIMockExecutor.REFELCTION_IMPL;
		return this;
	}

	/**
	 * Sets the version of soapui to use. Obviously this is dependent on the
	 * version of soapui this library was compiled against. if not specified, it
	 * will use the default version with which this module was compiled with.
	 * 
	 * Check which version is default by running {@link SoapUI#version()}.
	 * 
	 * @param version
	 *            the soapui version to use (note it currently does not do any
	 *            checks so one could force down anything)
	 * @return
	 */
	public SoapUIMockRunner soapuiVersion(String version) {
		this.soapuiVersion = version;
		return this;
	}

	public String getMockEndpoint() {
		return task.getMockEndpoint();
	}

	public SoapUIMockRunner withImplementation(String implName) {
		this.implName = implName;
		return this;
	}

	public boolean isRunning() {
		return runner != null && runner.isRunning();
	}

	public SoapUIMockRunner includeUnitTestLocation() {
		includeUnitTestLocation = true;
		return this;
	}

	public List<String> getPassFilters() {
		if (passFilters == null) {
			passFilters = new ArrayList<String>();
		}
		return passFilters;
	}

	public SoapUIMockRunner addPassFilters(String... filters) {
		if (filters != null && filters.length > 0) {
			getPassFilters().addAll(Arrays.asList(filters));
		}
		return this;
	}

	public List<String> getBlockFilters() {
		if (blockFilters == null) {
			blockFilters = new ArrayList<String>();
		}
		return blockFilters;
	}

	public SoapUIMockRunner addBlockFilters(String... filters) {
		if (filters != null && filters.length > 0) {
			getBlockFilters().addAll(Arrays.asList(filters));
		}
		return this;
	}

	public SoapUIMockRunner setProxy(String type, String host, int port,
			String username, String password) {

		Authentication auth = new AuthenticationBuilder().addUsername(username)
				.addPassword(password).build();

		resolver.setProxy(new Proxy(type, host, port, auth));

		return this;
	}

}
