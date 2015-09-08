package org.walkmod.maven.providers;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.maven.settings.Settings;
import org.jboss.shrinkwrap.resolver.api.maven.pom.ParsedPomFile;
import org.jboss.shrinkwrap.resolver.impl.maven.MavenWorkingSessionImpl;
import org.jboss.shrinkwrap.resolver.impl.maven.bootstrap.MavenSettingsBuilder;
import org.walkmod.conf.ConfigurationException;

public class LocalMavenRepository {

	private Settings settings = new MavenSettingsBuilder()
			.buildDefaultSettings();

	private String localRepo = settings.getLocalRepository();

	public File getLocalRepoDir(String groupId, String artifactId,
			String version) {

		String subDir = groupId.replaceAll("\\.", "/");
		File localRepoDir = new File(localRepo, subDir + File.separator
				+ artifactId + File.separator + version);
		localRepoDir.mkdirs();
		return localRepoDir;
	}

	public void installPom(File pom) {
		try {
			ParsedPomFile parsedPom = getParsedPomFile(pom);

			File localRepoDir = getLocalRepoDir(parsedPom.getGroupId(),
					parsedPom.getArtifactId(), parsedPom.getVersion());

			FileUtils.copyFile(pom,
					new File(localRepoDir, parsedPom.getArtifactId() + "-"
							+ parsedPom.getVersion() + ".pom"));
		} catch (IOException e) {
			throw new ConfigurationException(
					"Error creating the parent pom into the local repo");
		}
	}
	
	public void installArtifact(File jarFile, ParsedPomFile pom) throws IOException{
		File localRepoDir = getLocalRepoDir(pom.getGroupId(),
				pom.getArtifactId(), pom.getVersion());
		FileUtils.copyFile(jarFile,
				new File(localRepoDir, pom.getArtifactId() + "-"
						+ pom.getVersion() + ".jar"));
		FileUtils.copyFile(new File(pom.getBaseDirectory(), "pom.xml"),
				new File(localRepoDir, pom.getArtifactId() + "-"
						+ pom.getVersion() + ".pom"));
	}
	
	public ParsedPomFile getParsedPomFile(File pom) {

		MavenWorkingSessionImpl session = new MavenWorkingSessionImpl();
		session.useLegacyLocalRepository(true);
		session.loadPomFromFile(pom);
		return session.getParsedPomFile();

	}

}
