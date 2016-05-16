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
import java.util.HashSet;
import java.util.Set;

import org.walkmod.conf.ConfigurationException;
import org.walkmod.conf.ConfigurationProvider;
import org.walkmod.conf.entities.Configuration;

public class ClassLoaderConfigurationProvider implements ConfigurationProvider {

	private File pomFile = null;

	private Configuration configuration;

	private boolean compile = true;

	private ClassLoader cl;

	private LocalMavenRepository localRepo;
	
	private String mavenArgs="";

	public ClassLoaderConfigurationProvider() {
		this(new File("pom.xml"), false, Thread.currentThread()
				.getContextClassLoader(), null, null);
	}

	public ClassLoaderConfigurationProvider(File pomFile) {
		this(pomFile, false, Thread.currentThread().getContextClassLoader(),
				null, null);
	}

	public ClassLoaderConfigurationProvider(File pomFile, boolean buildJar,
			ClassLoader cl, Set<MavenModule> modules,
			Configuration configuration) {
		setPomFile(pomFile);
		this.cl = cl;
		this.configuration = configuration;
		localRepo = new LocalMavenRepository();
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
	
	@Override
	public void load() throws ConfigurationException {
		if (configuration != null) {
			if (configuration.getClassLoader() != null) {
				cl = configuration.getClassLoader();
			}

			MavenProject mvnProject = new MavenProject(pomFile,
					new HashSet<MavenModule>(), localRepo, isCompile(),
					cl, mavenArgs);
			mvnProject.clean();
			try {
				configuration.getParameters().put("classLoader",
						mvnProject.resolveClassLoader());

			} catch (Exception e1) {
				throw new ConfigurationException(e1.getMessage(), e1);
			}

		}
	}

	public boolean isCompile() {
		return compile;
	}

	public void setCompile(boolean compile) {
		this.compile = compile;
	}
	
	public void setMavenArgs(String mvnArgs){
	   this.mavenArgs = mvnArgs;
	}

}
