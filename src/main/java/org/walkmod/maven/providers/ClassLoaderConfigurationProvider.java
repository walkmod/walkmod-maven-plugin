package org.walkmod.maven.providers;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolvedArtifact;
import org.jboss.shrinkwrap.resolver.api.maven.ScopeType;
import org.walkmod.conf.ConfigurationException;
import org.walkmod.conf.ConfigurationProvider;
import org.walkmod.conf.entities.Configuration;

public class ClassLoaderConfigurationProvider implements ConfigurationProvider {

	private File pomFile = null;

	private List<String> classPathEntries = new LinkedList<String>();

	private Configuration configuration;

	public ClassLoaderConfigurationProvider() {
		this(new File("pom.xml"));
	}

	public ClassLoaderConfigurationProvider(File pomFile) {
		setPomFile(pomFile);
		classPathEntries.add("target/classes");
		classPathEntries.add("target/test-classes");
	}

	public List<MavenResolvedArtifact> resolve() {
		if (pomFile != null) {
			if (pomFile.exists()) {

				MavenResolvedArtifact[] artifacts = Maven.resolver()
						.loadPomFromFile(getPomFile())
						.importDependencies(ScopeType.COMPILE, ScopeType.TEST)
						.resolve().withTransitivity().asResolvedArtifact();
				return Arrays.asList(artifacts);
			} else {
				throw new ConfigurationException("The pom.xml file at ["
						+ pomFile.getAbsolutePath() + "] does not exists");
			}
		} else {
			throw new ConfigurationException("The pom.xml file is undefined");
		}

	}

	public File getPomFile() {
		return pomFile;
	}

	public void setPomFile(File file) {
		this.pomFile = file;
	}

	@Override
	public void init(Configuration configuration) {
		this.configuration = configuration;
	}

	public void setClassPathEntries(List<String> classPathEntries) {
		this.classPathEntries = classPathEntries;
	}

	@Override
	public void load() throws ConfigurationException {
		if (configuration != null) {

			List<MavenResolvedArtifact> artifacts = resolve();

			if (artifacts != null) {
				URL[] classPath = new URL[artifacts.size()
						+ classPathEntries.size()];
				int i = 0;
				for (MavenResolvedArtifact mra : artifacts) {
					try {
						classPath[i] = mra.asFile().toURI().toURL();
					} catch (MalformedURLException e) {
						throw new ConfigurationException(
								"Invalid URL for the dependency "
										+ mra.asFile().getAbsolutePath(),
								e.getCause());
					}
					i++;
				}
				for (String entry : classPathEntries) {
					try {

						classPath[i] = new File(entry).toURI().toURL();

					} catch (MalformedURLException e) {
						throw new ConfigurationException(
								"Invalid URL for the classpath entry "
										+ new File(entry).getAbsolutePath(),
								e.getCause());
					}
					i++;
				}
				URLClassLoader loader = new URLClassLoader(classPath);
				configuration.getParameters().put("classLoader", loader);
			}

		}
	}

}
