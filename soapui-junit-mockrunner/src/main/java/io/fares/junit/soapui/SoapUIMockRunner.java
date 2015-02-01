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

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import io.fares.junit.classloader.ClassLoaderFactory;
import io.fares.junit.classloader.FilteringClassLoaderFactory;

public class SoapUIMockRunner implements TestRule {

	private String implName = SoapUIMockExecutor.SIMPLE_IMPL;

	private MockRunnerTask task = new MockRunnerTask();

	/**
	 * Need to keep track of the executing runner
	 */
	private SoapUIMockExecutor runner;

	public Statement apply(Statement base, Description description) {
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

		ClassLoaderFactory clf = new FilteringClassLoaderFactory();

		// create a jailing classloader that allows interfaces to pass to parent
		// but restricts the impl
		ClassLoader cl = clf.createClassLoader(getClass().getClassLoader(),
				SoapUIMock.class);

		runner = new SoapUIMockExecutor(cl,
				implName == null ? SoapUIMockExecutor.SIMPLE_IMPL : implName);

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

}
