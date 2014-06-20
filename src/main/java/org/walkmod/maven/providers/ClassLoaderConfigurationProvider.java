/* 
  Copyright (C) 2013 Raquel Pau and Albert Coroleu.
 
 Walkmod is free software: you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 
 Walkmod is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser General Public License for more details.
 
 You should have received a copy of the GNU Lesser General Public License
 along with Walkmod.  If not, see <http://www.gnu.org/licenses/>.*/
package org.walkmod.maven.providers;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.jboss.shrinkwrap.resolver.api.Resolvers;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolvedArtifact;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolverSystem;
import org.jboss.shrinkwrap.resolver.api.maven.ScopeType;
import org.walkmod.conf.ConfigurationException;
import org.walkmod.conf.ConfigurationProvider;
import org.walkmod.conf.entities.Configuration;

public class ClassLoaderConfigurationProvider implements ConfigurationProvider {

	private File pomFile = null;

	private List<String> classPathEntries = new LinkedList<String>();

	private Configuration configuration;

	private ClassLoader classLoader = Thread.currentThread()
			.getContextClassLoader();

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

				MavenResolvedArtifact[] artifacts = Resolvers
						.use(MavenResolverSystem.class, classLoader)
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
			if (configuration.getClassLoader() != null) {
				classLoader = configuration.getClassLoader();
			}
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
