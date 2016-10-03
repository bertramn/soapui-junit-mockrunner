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
import java.util.List;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;

public interface ClasspathResolver {

	public Proxy getProxy();

	public void setProxy(Proxy proxy);

	public ClasspathResolver withProxy(Proxy proxy);

	public List<Artifact> getArtifacts();

	public void setArtifacts(List<Artifact> artifacts);

	public ClasspathResolver addArtifacts(List<Artifact> artifacts);

	public ClasspathResolver addArtifact(Artifact... artifact);

	public List<RemoteRepository> getRemoteRepositories();

	public void setRemoteRepositories(List<RemoteRepository> repositories);

	public ClasspathResolver addRemoteRepositories(
			List<RemoteRepository> repositories);

	public ClasspathResolver addRemoteRepository(
			RemoteRepository... repositories);

	public List<URL> resolveClassPath();

}
