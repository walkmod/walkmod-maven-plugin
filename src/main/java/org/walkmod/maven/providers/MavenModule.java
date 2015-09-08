package org.walkmod.maven.providers;

import java.io.File;

import org.apache.maven.model.Model;
import org.walkmod.conf.ConfigurationException;

public class MavenModule {

	private String name;
	private Model mavenModel;
	private File directory;
	private MavenProject loader;
	private boolean compiled = false;
	private boolean submodulesProcessed = false;

	public MavenModule(String name, Model mavenModel, File directory,
			MavenProject loader) {
		this.name = name;
		this.mavenModel = mavenModel;
		this.directory = directory;
		this.loader = loader;
	}
	
	public String getName(){
		return name;
	}

	public String getArtifactId() {
		return mavenModel.getArtifactId();
	}

	public String getGroupId() {
		if (mavenModel.getGroupId() == null) {
			return mavenModel.getParent().getGroupId();
		}
		return mavenModel.getGroupId();
	}
	
	public File getDirectory(){
		return directory;
	}

	public String name() {
		return name;
	}

	public void compile() throws Exception {
		if (!compiled) {
			loader.build();
		}
		compiled = true;
	}

	public void loadSubmodules() throws ConfigurationException {
		if (!submodulesProcessed) {
			loader.lookUpSubmodules();
		}
		submodulesProcessed = true;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof MavenModule) {
			return getGroupId().equals(((MavenModule) o).getGroupId())
					&& getArtifactId().equals(((MavenModule) o).getArtifactId());
		}
		return false;
	}
}
