package org.walkmod.maven.initializers;

import java.io.File;
import java.util.List;

import org.apache.maven.model.Model;
import org.walkmod.conf.Initializer;
import org.walkmod.conf.ProjectConfigurationProvider;
import org.walkmod.conf.entities.Configuration;
import org.walkmod.maven.providers.MavenProject;

public class MavenInitializer implements Initializer {

	@Override
	public void execute(ProjectConfigurationProvider provider) throws Exception {
		File parentDir = provider.getConfigurationFile().getCanonicalFile().getParentFile();
		File pomFile = new File(parentDir, "pom.xml");
		if (pomFile.exists()) {
			MavenProject mvnProject = new MavenProject(pomFile);

			Model model = mvnProject.getModel();

			List<String> modules = model.getModules();
			if (modules != null) {
				Configuration c = provider.getConfiguration();
				if(c != null){
					c.setModules(modules);
				}
				provider.addModules(modules);
				for (String module : modules) {
					File moduleDir = new File(parentDir, module);
					ProjectConfigurationProvider moduleCfgProvider = provider.clone(new File(moduleDir, "walkmod."
							+ provider.getFileExtension()));
					moduleCfgProvider.createConfig();
					execute(moduleCfgProvider);
				}
			}
		}

	}
}
