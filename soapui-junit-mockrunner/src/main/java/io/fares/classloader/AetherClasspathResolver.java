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

import java.io.File;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Named;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

import io.fares.aether.GuiceRepositorySystemFactory;
import io.fares.aether.LoggerStream;
import io.fares.aether.LoggingRepositoryListener;
import io.fares.aether.LoggingTransferListener;

@Named("AetherClasspathResolver")
public class AetherClasspathResolver implements ClasspathResolver {

	protected final Logger LOG = Logger
			.getLogger(getClass().getName());

	@Inject
	private RepositorySystem system;

	private Proxy proxy;

	private List<Artifact> artifacts = Collections.emptyList();

	List<RemoteRepository> remoteRepositories = Collections.emptyList();

	public RepositorySystem getRepositorySystem() {
		if (system == null) {
			system = GuiceRepositorySystemFactory.newRepositorySystem();
		}
		return system;
	}

	public void setRepositorySystem(RepositorySystem system) {
		this.system = system;
	}

	public AetherClasspathResolver withRepositorySystem(RepositorySystem system) {
		setRepositorySystem(system);
		return this;
	}

	@Override
	public Proxy getProxy() {
		return proxy;
	}

	@Override
	public void setProxy(Proxy proxy) {
		this.proxy = proxy;
	}

	@Override
	public AetherClasspathResolver withProxy(Proxy proxy) {
		setProxy(proxy);
		return this;
	}

	@Override
	public List<Artifact> getArtifacts() {
		return this.artifacts;
	}

	@Override
	public void setArtifacts(List<Artifact> artifacts) {
		if (artifacts != null) {
			if (this.artifacts.isEmpty()) {
				this.artifacts = artifacts;
			} else {
				this.artifacts.addAll(artifacts);
			}
		}
	}

	@Override
	public ClasspathResolver addArtifacts(List<Artifact> artifacts) {
		if (artifacts != null && !artifacts.isEmpty()) {
			setArtifacts(artifacts);
		}
		return this;
	}

	@Override
	public ClasspathResolver addArtifact(Artifact... artifacts) {
		if (artifacts != null && artifacts.length > 0) {
			setArtifacts(Arrays.asList(artifacts));
		}
		return this;
	}

	@Override
	public List<RemoteRepository> getRemoteRepositories() {
		return remoteRepositories;
	}

	@Override
	public void setRemoteRepositories(List<RemoteRepository> repositories) {
		if (repositories != null) {
			// if proxy is set, we need to add proxy to all
			if (proxy != null) {
				if (this.remoteRepositories.isEmpty()) {
					this.remoteRepositories = new ArrayList<RemoteRepository>(
							repositories.size());
				}
				for (RemoteRepository repository : repositories) {
					this.remoteRepositories.add(new RemoteRepository.Builder(
							repository).setProxy(proxy).build());
				}
			} else {
				// else we can just do the normal
				if (this.remoteRepositories.isEmpty()) {
					this.remoteRepositories = repositories;
				} else {
					this.remoteRepositories.addAll(repositories);
				}
			}
		}
	}

	@Override
	public ClasspathResolver addRemoteRepositories(
			List<RemoteRepository> repositories) {
		if (repositories != null && !repositories.isEmpty()) {
			setRemoteRepositories(repositories);
		}
		return this;
	}

	@Override
	public ClasspathResolver addRemoteRepository(
			RemoteRepository... repositories) {
		if (repositories != null && repositories.length > 0) {
			setRemoteRepositories(Arrays.asList(repositories));
		}
		return this;
	}

	@Override
	public List<URL> resolveClassPath() {

		// first get system
		RepositorySystem system = getRepositorySystem();
		// get a session
		DefaultRepositorySystemSession session = newRepositorySystemSession(system);

		// then make all configured remote repositories

		// hack for now add all artifacts as compile time dependencies as we
		// want to run this thing obviously
		List<Dependency> dependencies = new ArrayList<Dependency>(
				artifacts.size());

		for (Artifact artifact : artifacts) {
			dependencies.add(new Dependency(artifact, JavaScopes.COMPILE));
		}

		CollectRequest collectRequest = new CollectRequest(dependencies, null,
				getRemoteRepositories());

		// lets get all dependencies
		DependencyFilter classpathFilter = DependencyFilterUtils
				.classpathFilter(JavaScopes.COMPILE);

		// the final request for the repos
		DependencyRequest dependencyRequest = new DependencyRequest(
				collectRequest, classpathFilter);

		try {

			DependencyResult result = getRepositorySystem()
					.resolveDependencies(session, dependencyRequest);

			List<ArtifactResult> artifactResults = result.getArtifactResults();

			// we will be using a hashset first to ensure we only add one
			// element of each dependency
			Set<URL> results = new LinkedHashSet<URL>();

			for (ArtifactResult artifactResult : artifactResults) {
				Artifact a = artifactResult.getArtifact();
				// FIXME currently we can only add locally resolved artefacts
				if (a.getFile() != null && a.getFile().exists()) {
					// FIXME try/catch in a for loop ...
					try {
						URL depUrl = a.getFile().toURI().toURL();
						results.add(depUrl);
					} catch (MalformedURLException e) {
						LOG.warning("Cannot resolve "
								+ a.getFile().getAbsolutePath());
					}
				} else {
					LOG.warning("Can't respove artefact "
							+ (a.getFile() == null ? null : a.getFile()
									.getAbsoluteFile()));
				}
			}

			return Lists.newArrayList(results);

		} catch (DependencyResolutionException e) {
			throw new RuntimeException(e);
		}

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
		PrintStream logger = new LoggerStream(LOG, Level.INFO);
		session.setTransferListener(new LoggingTransferListener());
		session.setRepositoryListener(new LoggingRepositoryListener());

		// uncomment to generate dirty trees
		// session.setDependencyGraphTransformer( null );

		// ignore checksums on smartbear
		session.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_IGNORE);

		return session;
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

}
