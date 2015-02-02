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

import io.fares.classloader.ClassLoaderFactory;
import io.fares.junit.soapui.internal.ReflectionJUnitSoapUIRunner;
import io.fares.junit.soapui.internal.SimpleJUnitSoapUIRunner;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

// TODO annotate with "needs class loader factory"
public final class SoapUIMockExecutor implements SoapUIMock {

	public static final String REFELCTION_IMPL = ReflectionJUnitSoapUIRunner.class
			.getName();

	public static final String SIMPLE_IMPL = SimpleJUnitSoapUIRunner.class
			.getName();

	private static ExecutorService service = Executors.newCachedThreadPool();

	/**
	 * The mock runner delegate responsible for managing the actual SoapUI mock
	 * service execution
	 */
	private SoapUIMock delegate;

	/**
	 * The classloader factory which will be used to create the classloader that
	 * will in turn be used to load and execute the soapui mock
	 */
	private ClassLoaderFactory classLoaderFactory;

	/**
	 * The name of the {@link SoapUIMock} implementation used to load the
	 * delegate
	 */
	private String implClassName;

	protected SoapUIMockExecutor() {
	}

	public SoapUIMockExecutor(ClassLoaderFactory classLoaderFactory,
			String implClassName) {
		this.classLoaderFactory = classLoaderFactory;
		this.implClassName = implClassName;
	}

	@Override
	public void start(final MockRunnerTask task) {

		if (classLoaderFactory == null) {
			throw new RuntimeException(
					"A filtering classloader factory must be configured");
		}

		if (implClassName == null) {
			throw new RuntimeException(
					"A implementation class name must be configured");
		}

		try {

			// run up a future
			Future<SoapUIMock> srf = service
					.submit(new CallableRunner(classLoaderFactory
							.createClassLoader(), implClassName, task));

			// better to wait until this whole mock is loaded
			delegate = srf.get();

		} catch (Exception e) {
			throw new RuntimeException("Failed to start soapui runner thread",
					e);
		}

	}

	@Override
	public void stop() {
		if (delegate != null) {
			delegate.stop();
		}
	}

	@Override
	public boolean isRunning() {
		return delegate.isRunning();
	}

	private final class CallableRunner implements Callable<SoapUIMock> {

		ClassLoader filteringClassLoader;
		ClassLoader originalClassLoader;

		String implClassName;

		MockRunnerTask task;

		public CallableRunner(final ClassLoader filteringClassLoader,
				final String implClassName, MockRunnerTask task) {

			this.filteringClassLoader = filteringClassLoader;
			this.implClassName = implClassName;
			this.task = task;
		}

		@Override
		public SoapUIMock call() throws Exception {

			// swap thead context classloader in case soapui does something
			// weird
			originalClassLoader = Thread.currentThread()
					.getContextClassLoader();
			Thread.currentThread().setContextClassLoader(filteringClassLoader);

			try {
				@SuppressWarnings("unchecked")
				Class<SoapUIMock> rmeClass = (Class<SoapUIMock>) filteringClassLoader
						.loadClass(implClassName);

				SoapUIMock rme = rmeClass.newInstance();
				rme.start(task);

				return rme;
			} finally {
				if (originalClassLoader != null) {
					Thread.currentThread().setContextClassLoader(
							originalClassLoader);
				}
			}
		}

	}

}
