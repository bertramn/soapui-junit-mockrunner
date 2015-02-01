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

import java.io.File;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

import javax.inject.Named;
import com.google.inject.Inject;

import io.fares.aether.LoggingRepositoryListener;
import io.fares.aether.LoggingTransferListener;
import io.fares.aether.GuiceRepositorySystemFactory;
import io.fares.aether.LoggerStream;
import io.fares.junit.soapui.SoapUIMockExecutor;

@Named("FilteringClassLoaderFactory")
public class FilteringClassLoaderFactory implements ClassLoaderFactory {

	protected static final Logger LOG = Logger
			.getLogger(FilteringClassLoaderFactory.class.getName());

	@Inject
	private RepositorySystem system;

	public RepositorySystem getRepositorySystem() {
		if (system == null) {
			system = GuiceRepositorySystemFactory.newRepositorySystem();
		}
		return system;
	}

	public void setRepositorySystem(RepositorySystem system) {
		this.system = system;
	}

	@Override
	public ClassLoader createClassLoader() throws DependencyResolutionException {
		// we just add ourselves
		return createClassLoader(getClass().getClassLoader(), Object.class);
	}

	@Override
	public ClassLoader createClassLoader(ClassLoader parent,
			Class<?>... jailInclusions) throws DependencyResolutionException {

		List<URL> locations = new ArrayList<URL>((jailInclusions == null ? 0
				: jailInclusions.length));

		if (jailInclusions != null && jailInclusions.length > 0) {

			for (Class<?> jailInclusion : jailInclusions) {
				URL url = createUrlFromClassLocation(jailInclusion);
				if (url != null) {
					// check if we already have this covered
					if (!locations.contains(url)) {
						locations.add(url);
					}
				}
			}
		}

		return createClassLoader(parent, locations.toArray(new URL[] {}));

	}

	@Override
	public ClassLoader createClassLoader(ClassLoader parent,
			URL... jailInclusions) throws DependencyResolutionException {

		DefaultRepositorySystemSession session = newRepositorySystemSession(getRepositorySystem());

		// create a repo collection
		List<RemoteRepository> remoteRepos = newRepositories(
				getRepositorySystem(), session);

		// lets get all dependencies
		DependencyFilter classpathFlter = DependencyFilterUtils
				.classpathFilter(JavaScopes.COMPILE);

		CollectRequest collectRequest = new CollectRequest();
		collectRequest.setRoot(new Dependency(newSoapUIArtifact(),
				JavaScopes.COMPILE));
		collectRequest.setRepositories(remoteRepos);

		DependencyRequest dependencyRequest = new DependencyRequest(
				collectRequest, classpathFlter);

		List<ArtifactResult> artifactResults = getRepositorySystem()
				.resolveDependencies(session, dependencyRequest)
				.getArtifactResults();

		List<URL> jailDependencies = new ArrayList<URL>();
		for (ArtifactResult artifactResult : artifactResults) {
			Artifact a = artifactResult.getArtifact();
			// System.out.println(a + " resolved to " + a.getFile());
			if (a.getFile() != null && a.getFile().exists()) {
				try {
					jailDependencies.add(a.getFile().toURI().toURL());
				} catch (MalformedURLException e) {
					System.err.println("Cannot resolve "
							+ a.getFile().getAbsolutePath());
				}
			} else {
				System.err
						.println("Can't respove artefact " + a.getFile() == null ? null
								: a.getFile().getAbsoluteFile());
			}
		}

		// add any jail inclusions
		// TODO also need to add something that would allow soapui ext folders
		if (jailInclusions != null) {
			jailDependencies.addAll(Arrays.asList(jailInclusions));
		}

		// wire containerRealm with imports
		URL[] all = jailDependencies.toArray(new URL[jailDependencies.size()]);

		FilteringClassLoader fwcl = new FilteringClassLoader(parent,
				SoapUIMockExecutor.DEFAULT_PASSFILTER,
				SoapUIMockExecutor.DEFAULT_BLOCKFILTER);

		return new URLClassLoader(all, fwcl);

	}

	public DefaultRepositorySystemSession newRepositorySystemSession() {
		return newRepositorySystemSession(getRepositorySystem());
	}

	private DefaultRepositorySystemSession newRepositorySystemSession(
			RepositorySystem system) {

		DefaultRepositorySystemSession session = MavenRepositorySystemUtils
				.newSession();

		// every container must have one
		session.setLocalRepositoryManager(system.newLocalRepositoryManager(
				session, findLocalRepository()));

		// logging for the activity
		PrintStream fineps = new LoggerStream(LOG, Level.FINE);
		session.setTransferListener(new LoggingTransferListener(fineps));
		session.setRepositoryListener(new LoggingRepositoryListener(fineps));

		// uncomment to generate dirty trees
		// session.setDependencyGraphTransformer( null );

		// ignore checksums on smartbear
		session.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_IGNORE);

		return session;
	}

	public List<RemoteRepository> newRepositories() {
		RepositorySystem reposys = getRepositorySystem();
		return newRepositories(reposys, newRepositorySystemSession(reposys));
	}

	private List<RemoteRepository> newRepositories(RepositorySystem system,
			RepositorySystemSession session) {
		return new ArrayList<RemoteRepository>(
				Arrays.asList(newSoapUIRepository()));
	}

	public final RemoteRepository newSoapUIRepository() {
		return new RemoteRepository.Builder("soapui", "default",
				"http://www.soapui.org/repository/maven2/").build();
	}

	public final Artifact newSoapUIArtifact() {
		return new DefaultArtifact("com.smartbear.soapui:soapui:5.0.0");
	}

	public final String getBasedir() {
		return System.getProperty("basedir", new File("").getAbsolutePath());

	}

	protected LocalRepository findLocalRepository() {

		LocalRepository localRepo = null;

		// first try M2_REPO
		String repoHome = System.getenv("M2_REPO");
		if (repoHome != null) {
			File f = new File(repoHome);
			if (f.exists()) {
				localRepo = new LocalRepository(f);
			}
		}

		// then user home
		if (localRepo == null) {
			String userHome = System.getProperty("user.home");
			if (userHome != null) {
				File fh = new File(userHome, ".m2" + File.separator
						+ "repository");
				if (fh.exists()) {
					localRepo = new LocalRepository(fh);
				}
			}
		}

		// if all fails we'll just create one in target of the program
		if (localRepo == null) {
			localRepo = new LocalRepository("target/soapui-runner-repo");
		}

		return localRepo;
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
