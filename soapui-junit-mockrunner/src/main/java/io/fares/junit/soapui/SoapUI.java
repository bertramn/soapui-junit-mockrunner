package io.fares.junit.soapui;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;

public class SoapUI {

	protected final static Logger LOG = Logger
			.getLogger(SoapUI.class.getName());

	public static final String[] DEFAULT_PASSFILTER = new String[] { //
	"java.", //
			"javax.swing.", //
			"javax.crypto.", //
			"javax.net.", //
			"sun.", //
			"javax.security.", //
			"org.unitils.", //
			"java.util.logging.", //
			SoapUIMock.class.getPackage().getName() + '.' };

	public static final String[] DEFAULT_BLOCKFILTER = new String[] {
			"com.eviware.", "org.apache.",
			SoapUIMock.class.getPackage().getName() + ".internal." };

	private static Properties appProperties;

	/**
	 * @return the version of SoapUI this junit plugin was compiled against
	 */
	public static String version() {

		if (appProperties == null) {
			appProperties = loadProperties();
		}

		return appProperties.getProperty("soapui.version");
	}

	private static Properties loadProperties() {
		Properties props = new Properties();
		InputStream in = SoapUI.class
				.getResourceAsStream("/soapui.dep.properties");
		if (in == null) {
			throw new RuntimeException(
					"Could not find [soapui.dep.properties] file in classpath");
		} else {
			try {
				props.load(in);
			} catch (IOException e) {
				LOG.log(Level.SEVERE,
						"unable to load default application properties", e);
				throw new RuntimeException(
						"unable to load default application properties", e);
			} finally {
				try {
					in.close();
				} catch (Exception ignore) {
				}
			}
		}
		return props;
	}

	public static final RemoteRepository newCentralRepository() {
		return new RemoteRepository.Builder("central", "default",
				"https://repo1.maven.org/maven2/").build();
	}

	public static final RemoteRepository newSoapUIRepository() {
		return new RemoteRepository.Builder("soapui", "default",
				"https://www.soapui.org/repository/maven2/").build();
	}

	/**
	 * Will create the soapui artifact that is required to run soapui mocks with
	 * the default version. See {@link #version()}.
	 * 
	 * @return soapui artifact descriptor
	 */
	public static final Artifact newSoapUIArtifact() {
		return newSoapUIArtifact(null);
	}

	/**
	 * Will create the soapui artifact that is required to run soapui mocks.
	 * 
	 * @param version
	 *            the specific version of soapui, if null it will just use
	 *            default {@link #version()}
	 * 
	 * @return soapui artifact descriptor
	 */
	public static final Artifact newSoapUIArtifact(String version) {
		return new DefaultArtifact("com.smartbear.soapui", "soapui", "jar",
				(version != null ? version : version()));
	}

}
