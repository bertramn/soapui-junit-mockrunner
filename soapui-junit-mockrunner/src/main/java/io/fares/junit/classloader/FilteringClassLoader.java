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
package io.fares.junit.classloader;

import java.security.SecureClassLoader;

/**
 * Refer to <a href=
 * "https://cxf.apache.org/javadoc/latest/index.html?org/apache/cxf/common/classloader/FireWallClassLoader.html"
 * >FireWallClassLoader</a> in CXF project.
 * 
 * @author Shamelessly plugged from CXF
 *
 */
public class FilteringClassLoader extends SecureClassLoader {
	private final String[] filters;
	private final String[] fnFilters;
	private final String[] negativeFilters;
	private final String[] negativeFNFilters;

	/**
	 * Constructor.
	 * 
	 * @param parent
	 *            The Parent ClassLoader to use.
	 * @param fs
	 *            A set of filters to let through. The filters and be either in
	 *            package form (<CODE>org.omg.</CODE> or <CODE>org.omg.*</CODE>)
	 *            or specify a single class (
	 *            <CODE>junit.framework.TestCase</CODE>).
	 *            <P>
	 *            When the package form is used, all classed in all subpackages
	 *            of this package are let trough the firewall. When the class
	 *            form is used, the filter only lets that single class through.
	 *            Note that when that class depends on another class, this class
	 *            does not need to be mentioned as a filter, because if the
	 *            originating class is loaded by the parent classloader, the
	 *            FireWallClassLoader will not receive requests for the
	 *            dependant class.
	 */
	public FilteringClassLoader(ClassLoader parent, String[] fs) {
		this(parent, fs, new String[0]);
	}

	/**
	 * Constructor.
	 * 
	 * @param parent
	 *            The Parent ClassLoader to use.
	 * @param fs
	 *            A set of filters to let through. The filters and be either in
	 *            package form (<CODE>org.omg.</CODE> or <CODE>org.omg.*</CODE>)
	 *            or specify a single class (
	 *            <CODE>junit.framework.TestCase</CODE>).
	 *            <P>
	 *            When the package form is used, all classed in all subpackages
	 *            of this package are let trough the firewall. When the class
	 *            form is used, the filter only lets that single class through.
	 *            Note that when that class depends on another class, this class
	 *            does not need to be mentioned as a filter, because if the
	 *            originating class is loaded by the parent classloader, the
	 *            FireWallClassLoader will not receive requests for the
	 *            dependant class.
	 * @param negativeFs
	 *            List of negative filters to use. Negative filters take
	 *            precedence over positive filters. When a class or resource is
	 *            requested that matches a negative filter it is not let through
	 *            the firewall even if an allowing filter would exist in the
	 *            positive filter list.
	 */
	public FilteringClassLoader(ClassLoader parent, String[] fs,
			String[] negativeFs) {
		super(parent);

		this.filters = processFilters(fs);
		this.negativeFilters = processFilters(negativeFs);

		this.fnFilters = filters2FNFilters(this.filters);
		this.negativeFNFilters = filters2FNFilters(this.negativeFilters);

		boolean javaCovered = false;
		if (this.filters == null) {
			javaCovered = true;
		} else {
			for (int i = 0; i < this.filters.length; i++) {
				if (this.filters[i].equals("java.")) {
					javaCovered = true;
				}
			}
		}

		if (this.negativeFilters != null) {
			String java = "java.";
			// try all that would match java: j, ja, jav, java and java.
			for (int i = java.length(); i >= 0; i--) {
				for (int j = 0; j < this.negativeFilters.length; j++) {
					if (negativeFilters[j].equals(java.substring(0, i))) {
						javaCovered = false;
					}
				}
			}
		}

		if (!javaCovered) {
			throw new SecurityException("It's unsafe to construct a "
					+ "FireWallClassLoader that does not let the java. "
					+ "package through.");
		}
	}

	private static String[] processFilters(String[] fs) {
		if (fs == null || fs.length == 0) {
			return null;
		}

		String[] f = new String[fs.length];
		for (int i = 0; i < fs.length; i++) {
			String filter = fs[i];
			if (filter.endsWith("*")) {
				filter = filter.substring(0, filter.length() - 1);
			}
			f[i] = filter;
		}
		return f;
	}

	private static String[] filters2FNFilters(String[] fs) {
		if (fs == null || fs.length == 0) {
			return null;
		}

		String[] f = new String[fs.length];
		for (int i = 0; i < fs.length; i++) {
			f[i] = fs[i].replace('.', '/');
		}
		return f;
	}

	protected Class<?> loadClass(String name, boolean resolve)
			throws ClassNotFoundException {
		if (negativeFilters != null) {
			for (int i = 0; i < negativeFilters.length; i++) {
				if (name.startsWith(negativeFilters[i])) {
					throw new ClassNotFoundException(name);
				}
			}
		}

		if (filters != null) {
			for (int i = 0; i < filters.length; i++) {
				if (name.startsWith(filters[i])) {
					return super.loadClass(name, resolve);
				}
			}
		} else {
			return super.loadClass(name, resolve);
		}
		throw new ClassNotFoundException(name);
	}

	/*
	 * protected Class<?> findClass(String name) throws ClassNotFoundException {
	 * if (negativeFilters != null) { for (int i = 0; i <
	 * negativeFilters.length; i++) { if (name.startsWith(negativeFilters[i])) {
	 * throw new ClassNotFoundException(name); } } } if (filters != null) { for
	 * (int i = 0; i < filters.length; i++) { if (name.startsWith(filters[i])) {
	 * return super.findClass(name); } } } else { return super.loadClass(name);
	 * } throw new ClassNotFoundException(name); }
	 */

	public java.net.URL getResource(String name) {
		if (negativeFNFilters != null) {
			for (int i = 0; i < negativeFNFilters.length; i++) {
				if (name.startsWith(negativeFNFilters[i])) {
					return null;
				}
			}
		}

		if (fnFilters != null) {
			for (int i = 0; i < fnFilters.length; i++) {
				if (name.startsWith(fnFilters[i])) {
					return super.getResource(name);
				}
			}
		} else {
			return super.getResource(name);
		}
		return null;
	}

	/**
	 * Returns the list of filters used by this FireWallClassLoader. The list is
	 * a copy of the array internally used.
	 * 
	 * @return The filters used.
	 */
	public String[] getFilters() {
		if (filters == null) {
			return null;
		}

		return filters.clone();
	}

	/**
	 * Returns the list of negative filters used by this FireWallClassLoader.
	 * The list is a copy of the array internally used.
	 * 
	 * @return The filters used.
	 */
	public String[] getNegativeFilters() {
		if (negativeFilters == null) {
			return null;
		}

		return negativeFilters.clone();
	}

}