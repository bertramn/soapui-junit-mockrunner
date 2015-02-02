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
