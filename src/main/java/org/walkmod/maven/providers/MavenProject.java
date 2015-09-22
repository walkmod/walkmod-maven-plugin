package org.walkmod.maven.providers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerResult;
import org.codehaus.plexus.compiler.javac.JavacCompiler;
import org.jboss.shrinkwrap.resolver.api.Resolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolvedArtifact;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolverSystem;
import org.jboss.shrinkwrap.resolver.api.maven.ScopeType;
import org.jboss.shrinkwrap.resolver.api.maven.pom.ParsedPomFile;
import org.jboss.shrinkwrap.resolver.impl.maven.MavenResolverSystemImpl;
import org.jboss.shrinkwrap.resolver.impl.maven.archive.plugins.CompilerPluginConfiguration;
import org.walkmod.conf.ConfigurationException;

public class MavenProject {

	private File sourceDir;

	private File buildDir;

	private ParsedPomFile pom;

	private List<MavenResolvedArtifact> artifacts;

	private File pomFile;

	private Set<MavenModule> modules;

	private LocalMavenRepository localRepo;

	private ClassLoader cl;

	private boolean requiresCompilation = true;

	private boolean requiresJarFile = true;

	public MavenProject(File pomFile, Set<MavenModule> modules,
			LocalMavenRepository localRepo, boolean requiresCompilation,
			boolean requiresJarFile, ClassLoader cl) {
		this.pomFile = new File(pomFile.getAbsolutePath());
		this.modules = modules;
		this.localRepo = localRepo;
		this.pom = localRepo.getParsedPomFile(pomFile);
		this.sourceDir = pom.getSourceDirectory();
		this.buildDir = new File(pom.getBaseDirectory(), "target/classes");
		this.cl = cl;
		this.requiresCompilation = requiresCompilation;
		this.requiresJarFile = requiresJarFile;
	}

	public MavenProject(File pomFile) {
		this(pomFile, new HashSet<MavenModule>(), new LocalMavenRepository(),
				true, false, Thread.currentThread().getContextClassLoader());
	}

	public void buildSources() throws CompilerException {
		build(sourceDir, new File(pom.getBaseDirectory(), "target/classes"));
	}

	public void buildTests() throws CompilerException {
		build(sourceDir,
				new File(pom.getBaseDirectory(), "target/test-classes"));
	}

	public void build(File sourceDir, File outputDir) throws CompilerException {
		if (sourceDir.exists()) {
			JavacCompiler compiler = new JavacCompiler();
			CompilerConfiguration configuration = new CompilerPluginConfiguration(
					pom).asCompilerConfiguration();
			final Collection<MavenResolvedArtifact> artifactResults = getArtifacts();

			for (MavenResolvedArtifact artifact : artifactResults) {
				String classpathEntry = artifact.asFile().getAbsolutePath();
				configuration.addClasspathEntry(classpathEntry);

			}

			configuration.addClasspathEntry(outputDir.getAbsolutePath());
			configuration.addSourceLocation(sourceDir.getAbsolutePath());
			configuration.setOutputLocation(outputDir.getAbsolutePath());

			CompilerResult result = compiler.performCompile(configuration);
			if (!result.isSuccess()) {
				List<CompilerMessage> messages = result.getCompilerMessages();
				StringBuilder sb = new StringBuilder("Found ")
						.append(messages.size())
						.append(" problems while compiling the project")
						.append("\n");

				for (CompilerMessage problem : messages) {
					sb.append(problem).append("\n");
				}

				throw new CompilerException(sb.toString());
			}

		}
	}

	private Model getModel(File pom) throws ConfigurationException {
		try {
			Reader reader = new FileReader(pom);
			try {
				MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
				Model model = xpp3Reader.read(reader);
				return model;
			} finally {
				reader.close();
			}
		} catch (Exception e) {
			throw new ConfigurationException("Error parsing "
					+ pomFile.getAbsolutePath());
		}
	}

	protected void lookUpSubmodules() throws ConfigurationException {
		Model model = getModel(pomFile);
		localRepo.installPom(pomFile);
		List<String> moduleNames = model.getModules();
		if (moduleNames != null) {
			for (String module : moduleNames) {

				File pomModule = new File(new File(pomFile.getParentFile(),
						module), "pom.xml");
				MavenModule mavenModule = new MavenModule(module,
						getModel(pomModule), pomModule, new MavenProject(
								pomModule, modules, localRepo, true, true, cl));
				modules.add(mavenModule);

				mavenModule.loadSubmodules();

			}
		}
	}

	private void lookUpModules() throws ConfigurationException {

		Model model = getModel(pomFile);
		Parent parent = model.getParent();
		if (parent != null) {
			String path = parent.getRelativePath();
			if (path == null || "".equals(path)) {
				path = pomFile.getParentFile().getParentFile()
						.getAbsolutePath();

			} else {
				path = pomFile.getParentFile().getAbsolutePath()
						+ File.separator + path;

			}
			if (path != null) {
				if (!path.endsWith("pom.xml")) {
					path += File.separator + "pom.xml";
				}

				File parentPom;
				try {
					parentPom = new File(path).getCanonicalFile();
				} catch (IOException e) {
					throw new ConfigurationException(
							"Error interpreting the path " + path, e);
				}
				if (parentPom.exists()) {

					MavenProject parentConfProvider = new MavenProject(
							parentPom, modules, localRepo, true, true, cl);

					parentConfProvider.lookUpModules();

					model = getModel(parentPom);
					List<String> moduleNames = model.getModules();
					if (moduleNames != null) {
						for (String module : moduleNames) {

							File pomModule = new File(new File(
									parentPom.getParentFile(), module),
									"pom.xml");
							MavenModule mavenModule = new MavenModule(module,
									getModel(pomModule), pomModule,
									new MavenProject(pomModule, modules,
											localRepo, true, true, cl));
							modules.add(mavenModule);

							mavenModule.loadSubmodules();

						}
					}
					localRepo.installPom(parentPom);

				}
			}
		}

	}

	public void build() throws Exception {
		if (requiresCompilation) {

			buildSources();
			if (requiresJarFile) {

				mvnPackage();

			} else {
				buildTests();
			}

		}
	}

	public Model getModel() {
		return getModel(pomFile);
	}

	public List<MavenResolvedArtifact> getArtifacts() {

		if (pomFile.exists()) {
			if (artifacts == null) {

				Model pom = getModel(pomFile);
				List<Dependency> deps = pom.getDependencies();

				lookUpModules();

				if (modules != null) {

					for (Dependency artifact : deps) {

						boolean isModule = false;
						Iterator<MavenModule> it = modules.iterator();
						MavenModule module = null;
						while (it.hasNext() && !isModule) {
							module = it.next();
							isModule = module.getGroupId().equals(
									artifact.getGroupId())
									&& module.getArtifactId().equals(
											artifact.getArtifactId());
						}
						if (isModule) {
							try {
								module.compile();
							} catch (Exception e) {
								throw new ConfigurationException(
										"Error compling the module "
												+ module.getName(), e);
							}
						}

					}

				}

				if (pom.getDependencies() != null
						&& !pom.getDependencies().isEmpty()) {
					MavenResolverSystemImpl mrs = (MavenResolverSystemImpl) Resolvers
							.use(MavenResolverSystem.class, cl);
					mrs.getMavenWorkingSession().useLegacyLocalRepository(true);

					MavenResolvedArtifact[] artifacts = mrs
							.loadPomFromFile(pomFile)
							.importDependencies(ScopeType.COMPILE,
									ScopeType.TEST, ScopeType.PROVIDED).resolve()
							.withTransitivity().asResolvedArtifact();

					this.artifacts = Arrays.asList(artifacts);
				} else {
					this.artifacts = new LinkedList<MavenResolvedArtifact>();
				}
			}
			return artifacts;
		} else {
			throw new ConfigurationException("The pom.xml file at ["
					+ pomFile.getAbsolutePath() + "] does not exists");
		}

	}

	public void mvnPackage() throws IOException {
		Collection<File> files = FileUtils.listFiles(buildDir,
				new String[] { "class" }, true);

		// Create a buffer for reading the files
		byte[] buf = new byte[1024];

		File aux = new File(pom.getBaseDirectory(), "target/"
				+ pom.getArtifactId() + "-" + pom.getVersion() + ".jar");

		String target = aux.getAbsolutePath();
		// Create the ZIP file

		JarOutputStream out = new JarOutputStream(new FileOutputStream(target));
		try {
			Iterator<File> it = files.iterator();
			// Compress the files
			while (it.hasNext()) {
				File source = it.next();
				FileInputStream in = new FileInputStream(source);

				String path = source.getAbsolutePath();
				String base = buildDir.getAbsolutePath();

				String relative = new File(base).toURI()
						.relativize(new File(path).toURI()).getPath();

				// Add ZIP entry to output stream.
				out.putNextEntry(new JarEntry(relative));

				// Transfer bytes from the file to the ZIP file
				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}

				// Complete the entry
				out.closeEntry();
				in.close();
			}
		} finally {

			// Complete the ZIP file
			out.close();
		}
		localRepo.installArtifact(aux, pom);
	}

	public URLClassLoader resolveClassLoader() throws Exception {

		build();
		List<String> classPathEntries = new LinkedList<String>();
		classPathEntries.add("target/classes");
		classPathEntries.add("target/test-classes");

		List<MavenResolvedArtifact> artifacts = getArtifacts();

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

			return new URLClassLoader(classPath);
		}
		return null;
	}
}
