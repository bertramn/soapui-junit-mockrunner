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
package io.fares.classloader;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Named;

@Named("FilteringClassLoaderFactory")
public class FilteringClassLoaderFactory implements ClassLoaderFactory {

	protected static final Logger LOG = Logger
			.getLogger(FilteringClassLoaderFactory.class.getName());

	ClasspathResolver classpathResolver;

	ClassLoader parentClassLoader;

	List<String> passFilters;
	List<String> blockFilters;
	List<URL> includeClasspathURLs;
	List<Class<?>> includeClazzContainerURLs;

	public FilteringClassLoaderFactory() {

	}

	public FilteringClassLoaderFactory(ClasspathResolver resolver) {
		this.classpathResolver = resolver;

	}

	public ClasspathResolver getClasspathResolver() {
		return classpathResolver;
	}

	public void setClasspathResolver(ClasspathResolver classpathResolver) {
		this.classpathResolver = classpathResolver;
	}

	public FilteringClassLoaderFactory withClasspathResolver(
			ClasspathResolver classpathResolver) {
		setClasspathResolver(classpathResolver);
		return this;
	}

	/**
	 * @return the parent classloader that the factory will use to put on the
	 *         bottom of the classloader chain (below the
	 *         {@link FilteringClassLoader}).
	 */
	public ClassLoader getParentClassLoader() {
		return parentClassLoader;
	}

	/**
	 * @param parentClassLoader
	 *            the parent classloader to use at the bottom of the hirarchical
	 *            classloader chain
	 */
	public void setParentClassLoader(ClassLoader parentClassLoader) {
		this.parentClassLoader = parentClassLoader;
	}

	public List<String> getPassFilters() {
		if (passFilters == null) {
			passFilters = new ArrayList<String>();
		}
		return passFilters;
	}

	/**
	 * Set the list of filters which will be used by the
	 * {@link FilteringClassLoader} to allow class or resource lookup requests
	 * through to the parent classloader.
	 * 
	 * 
	 * @param filters
	 *            a set of simple string filters which will override any other
	 *            filters set before
	 */
	public void setPassFilters(List<String> filters) {
		this.passFilters = filters;
	}

	/**
	 * Same as {@link #setAllowFilters(List)} only that this will add to
	 * existing filters instead of overriding them.
	 * 
	 * @param filters
	 *            a list of filters to add to the allow filters
	 * 
	 */
	public FilteringClassLoaderFactory addPassFilters(String... filters) {
		if (filters != null && filters.length > 0) {
			getPassFilters().addAll(Arrays.asList(filters));
		}
		return this;
	}

	/**
	 * See {@link #addPassFilters(String...)}
	 */
	public FilteringClassLoaderFactory addPassFilters(List<String> filters) {
		if (filters != null && filters.size() > 0) {
			getPassFilters().addAll(filters);
		}
		return this;
	}

	public List<String> getBlockFilters() {
		if (blockFilters == null) {
			blockFilters = new ArrayList<String>();
		}
		return blockFilters;
	}

	/**
	 * Set the list of filters which will be used by the
	 * {@link FilteringClassLoader} to block class or resource lookup requests
	 * through to the parent classloader. Please be aware block filters are
	 * always applied before allow filters. So make sure the filters in allow
	 * don't get blocked by a block filter first :).
	 * 
	 * @param filters
	 *            a set of simple string filters which will override any other
	 *            filters set before
	 */
	public void setBlockFilters(List<String> filters) {
		this.blockFilters = filters;
	}

	/**
	 * Same as {@link #setBlockFilters(List)} only that this will add to
	 * existing filters instead of overriding them.
	 * 
	 * @param filters
	 *            a list of filters to add to the block filter
	 * 
	 */
	public FilteringClassLoaderFactory addBlockFilters(String... filters) {
		if (filters != null && filters.length > 0) {
			getBlockFilters().addAll(Arrays.asList(filters));
		}
		return this;
	}

	public FilteringClassLoaderFactory addBlockFilters(List<String> filters) {
		if (filters != null && filters.size() > 0) {
			getBlockFilters().addAll(filters);
		}
		return this;
	}

	public List<Class<?>> getIncludeClazzContainerURLs() {
		if (includeClazzContainerURLs == null) {
			includeClazzContainerURLs = new ArrayList<Class<?>>();
		}
		return includeClazzContainerURLs;
	}

	public void setIncludeClazzContainerURLs(
			List<Class<?>> includeClazzContainerURLs) {
		this.includeClazzContainerURLs = includeClazzContainerURLs;
	}

	public FilteringClassLoaderFactory addIncludeClazzContainerURLs(
			Class<?>... includeClazzContainerURLs) {
		if (includeClazzContainerURLs != null
				&& includeClazzContainerURLs.length > 0) {
			getIncludeClazzContainerURLs().addAll(
					Arrays.asList(includeClazzContainerURLs));
		}
		return this;
	}

	public List<URL> getIncludeClasspathURLs() {
		if (includeClasspathURLs == null) {
			includeClasspathURLs = new ArrayList<URL>();
		}
		return includeClasspathURLs;
	}

	public void setIncludeClasspathURLs(List<URL> includeClasspathURLs) {
		this.includeClasspathURLs = includeClasspathURLs;
	}

	@Override
	public ClassLoader createClassLoader() {
		return createClassLoader(parentClassLoader != null ? parentClassLoader
				: getClass().getClassLoader());
	}

	@Override
	public ClassLoader createClassLoader(ClassLoader parent) {

		// lets give our classloader some locations to work with
		List<URL> jailDependencies = getIncludeClasspathURLs();

		// get all maven artefact urls
		List<URL> mavenDependencies = classpathResolver.resolveClassPath();

		// add them to the jail urls configured separately
		jailDependencies.addAll(mavenDependencies);

		// lets add all locations for these included clazzes too

		if (getIncludeClazzContainerURLs().size() > 0) {

			for (Class<?> clazz : getIncludeClazzContainerURLs()) {
				URL clazzUrl = createUrlFromClassLocation(clazz);
				if (clazzUrl != null && !jailDependencies.contains(clazzUrl)) {
					jailDependencies.add(clazzUrl);
				}
			}
		}

		// fire a filtering classloader with provided filters
		FilteringClassLoader fwcl = new FilteringClassLoader(parent,
				passFilters, blockFilters);

		// create a primary classloader with parent being the filtering
		// classloader blocking all classloading requests as per filter
		// configuration
		return new URLClassLoader(
				jailDependencies.toArray(new URL[jailDependencies.size()]),
				fwcl);

	}

	private URL createUrlFromClassLocation(Class<?> clazz) {

		// get meself
		String clazzName = "/" + clazz.getName().replace('.', '/') + ".class";
		String clazzLocation = clazz.getResource(clazzName).toExternalForm();

		// FIXME can only do file | jar atm
		if (clazzLocation.startsWith("file:")) {
			// trim off the rest
			clazzLocation = clazzLocation.substring(0, clazzLocation.length()
					- clazzName.length() + 1);
		} else if (clazzLocation.startsWith("jar:")) {
			clazzLocation = clazzLocation.substring(4, clazzLocation.length()
					- clazzName.length() - 1);
		} else {
			throw new UnsupportedOperationException(
					"desiphering jail resource " + clazzLocation
							+ " is not supported by this classloader factory");
		}
		// try to turn out the name of the inclusion
		try {
			return new URL(clazzLocation);
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Failed to find where the jail inclusion ["
					+ clazz.getName() + "] is served from", e);
			return null;
		}

	}

}
