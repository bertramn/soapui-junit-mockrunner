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

import io.fares.junit.soapui.internal.ReflectionJUnitSoapUIRunner;
import io.fares.junit.soapui.internal.SimpleJUnitSoapUIRunner;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class SoapUIMockExecutor implements SoapUIMock {

	public static final String REFELCTION_IMPL = ReflectionJUnitSoapUIRunner.class
			.getName();

	public static final String SIMPLE_IMPL = SimpleJUnitSoapUIRunner.class
			.getName();

	public static final String[] DEFAULT_PASSFILTER = new String[] { //
	"java.", //
			"javax.swing.", //
			"javax.crypto.", //
			"javax.net.", //
			"sun.", //
			"javax.security.", //
			"org.unitils.", //
			SoapUIMock.class.getPackage().getName() + '.' };

	public static final String[] DEFAULT_BLOCKFILTER = new String[] {
			"com.eviware.", "org.apache.",
			SoapUIMock.class.getPackage().getName() + ".internal." }

	;

	private static ExecutorService service = Executors.newCachedThreadPool();

	SoapUIMock delegate;

	ClassLoader filteringClassLoader;

	String implClassName;

	protected SoapUIMockExecutor() {
	}

	public SoapUIMockExecutor(ClassLoader filteringClassLoader, String implClassName) {
		this.filteringClassLoader = filteringClassLoader;
		this.implClassName = implClassName;
	}

	@Override
	public void start(final MockRunnerTask task) {

		if (filteringClassLoader == null) {
			throw new RuntimeException(
					"A filtering classloader must be configured");
		}

		if (implClassName == null) {
			throw new RuntimeException(
					"A implementation class name must be configured");
		}

		// run up a future
		Future<SoapUIMock> srf = service.submit(new CallableRunner(
				filteringClassLoader, implClassName, task));

		try {
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
