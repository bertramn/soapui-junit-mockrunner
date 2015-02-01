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
package io.fares.aether;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class LoggerStream extends PrintStream {

	private Level level = null;
	private Logger logger = null;

	/**
	 * A PrintStream that delegates to a Java Util Logger
	 * 
	 * @param logger
	 *            the {@link Logger} to be used for printing to this stream
	 * 
	 * @param level
	 *            The level at which to log at
	 */
	public LoggerStream(Logger logger, Level level) {
		super(new ByteArrayOutputStream(), true);
		this.logger = logger;
		this.level = level;
	}

	@Override
	public synchronized void flush() {
		super.flush();
		ByteArrayOutputStream stream = getStream();
		if (stream.size() == 0)
			return;
		String message = stream.toString();
		logger.log(level, message);
		stream.reset();
	}

	@Override
	public synchronized void println() {
		flush();
	}

	@Override
	public synchronized void println(Object x) {
		print(x);
		flush();
	}

	@Override
	public synchronized void println(String x) {
		print(x);
		flush();
	}

	@Override
	public synchronized void close() {
		flush();
		super.close();
	}

	@Override
	public synchronized boolean checkError() {
		flush();
		return super.checkError();
	}

	private ByteArrayOutputStream getStream() {
		return (ByteArrayOutputStream) this.out;
	}

}
