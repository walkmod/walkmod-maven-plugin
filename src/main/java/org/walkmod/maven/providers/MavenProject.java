package org.walkmod.maven.providers;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.maven.cli.MavenCli;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.jboss.shrinkwrap.resolver.api.Resolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolvedArtifact;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolverSystem;
import org.jboss.shrinkwrap.resolver.api.maven.ScopeType;
import org.jboss.shrinkwrap.resolver.api.maven.pom.ParsedPomFile;
import org.jboss.shrinkwrap.resolver.impl.maven.MavenResolverSystemImpl;
import org.walkmod.conf.ConfigurationException;

import com.google.common.base.Joiner;

public class MavenProject {

   private ParsedPomFile pom;

   private List<MavenResolvedArtifact> artifacts;

   private File pomFile;

   private Set<MavenModule> modules;

   private LocalMavenRepository localRepo;

   private ClassLoader cl;

   private boolean requiresCompilation = true;

   private String mvnArgs;

   public MavenProject(File pomFile, Set<MavenModule> modules, LocalMavenRepository localRepo,
         boolean requiresCompilation, ClassLoader cl, String mvnArgs) {
      this.pomFile = new File(pomFile.getAbsolutePath());
      this.modules = modules;
      this.localRepo = localRepo;
      this.pom = localRepo.getParsedPomFile(pomFile);
      this.cl = cl;
      this.requiresCompilation = requiresCompilation;
      this.mvnArgs = mvnArgs;
   }

   public MavenProject(File pomFile) {
      this(pomFile, new HashSet<MavenModule>(), new LocalMavenRepository(), true,
            Thread.currentThread().getContextClassLoader(), "");
   }

   public MavenProject(File pomFile, String mvnArgs) {
      this(pomFile, new HashSet<MavenModule>(), new LocalMavenRepository(), true,
            Thread.currentThread().getContextClassLoader(), mvnArgs);
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
         throw new ConfigurationException("Error parsing " + pomFile.getAbsolutePath());
      }
   }

   protected void lookUpSubmodules() throws ConfigurationException {
      Model model = getModel(pomFile);
      localRepo.installPom(pomFile);
      List<String> moduleNames = model.getModules();
      if (moduleNames != null) {
         for (String module : moduleNames) {

            File pomModule = new File(new File(pomFile.getParentFile(), module), "pom.xml");
            MavenModule mavenModule = new MavenModule(module, getModel(pomModule), pomModule,
                  new MavenProject(pomModule, modules, localRepo, true, cl, mvnArgs));
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
            path = pomFile.getParentFile().getParentFile().getAbsolutePath();

         } else {
            path = pomFile.getParentFile().getAbsolutePath() + File.separator + path;

         }
         if (path != null) {
            if (!path.endsWith("pom.xml")) {
               path += File.separator + "pom.xml";
            }

            File parentPom;
            try {
               parentPom = new File(path).getCanonicalFile();
            } catch (IOException e) {
               throw new ConfigurationException("Error interpreting the path " + path, e);
            }
            if (parentPom.exists()) {

               MavenProject parentConfProvider = new MavenProject(parentPom, modules, localRepo, true, cl, mvnArgs);

               parentConfProvider.lookUpModules();

               model = getModel(parentPom);
               List<String> moduleNames = model.getModules();
               if (moduleNames != null) {
                  for (String module : moduleNames) {

                     File pomModule = new File(new File(parentPom.getParentFile(), module), "pom.xml");
                     MavenModule mavenModule = new MavenModule(module, getModel(pomModule), pomModule,
                           new MavenProject(pomModule, modules, localRepo, true, cl, mvnArgs));
                     modules.add(mavenModule);

                     mavenModule.loadSubmodules();

                  }
               }
               localRepo.installPom(parentPom);

            }
         }
      }

   }

   private String[] addExtraArgs(String[] mvnArgs, String[] extraMvnArgs) {
      if (extraMvnArgs != null) {
         List<String> aux = new LinkedList<String>(Arrays.asList(mvnArgs));
         aux.addAll(Arrays.asList(extraMvnArgs));
         Iterator<String> it = aux.iterator();
         while (it.hasNext()) {
            String item = it.next().trim();
            if (item.equals("")) {
               it.remove();
            }
         }
         String[] result = new String[aux.size()];
         aux.toArray(result);
         return result;
      }
      return mvnArgs;
   }

   public void build() throws Exception {
      if (requiresCompilation) {

         Model model = getModel();
         Parent parent = model.getParent();
         int code;
         ClassWorld myClassWorld = new ClassWorld("plexus.core", cl);
         String path;
         String[] extraMvnArgs = null;
         if (this.mvnArgs != null) {
            this.mvnArgs = this.mvnArgs.trim();
            if (this.mvnArgs.length() > 0) {
               extraMvnArgs = this.mvnArgs.split(" ");
            }
         }
         String[] mvnArgs = addExtraArgs(new String[] { "clean", "install", "-DskipTests", "-DskipWalkmod" },
               extraMvnArgs);

         if (parent != null) {
            String relativePath = parent.getRelativePath();
            File aux = new File(pom.getBaseDirectory(), relativePath);

            path = aux.getParentFile().getAbsoluteFile().getCanonicalPath();
            File parentPomFile = new File(new File(path), "pom.xml");
            if (parentPomFile.exists()) {
               String moduleName = pom.getArtifactId();

               mvnArgs = addExtraArgs(new String[] { "clean", "install", "-pl", ":" + moduleName, "-am", "-DskipTests",
                     "-DskipWalkmod" }, extraMvnArgs);

               if (existsMvnCommand()) {
                  code = execMvnCommand(mvnArgs, path);
               } else {
                  String previousDir = System.getProperty("user.dir");
                  System.setProperty("user.dir", path);
                  code = MavenCli.doMain(mvnArgs, myClassWorld);
                  System.setProperty("user.dir", previousDir);
               }

            } else {
               path = pom.getBaseDirectory().getAbsolutePath();
               if (existsMvnCommand()) {
                  code = execMvnCommand(mvnArgs, path);
               } else {

                  code = MavenCli.doMain(mvnArgs, myClassWorld);
               }
            }

         } else {
            path = pom.getBaseDirectory().getAbsolutePath();
            if (existsMvnCommand()) {
               code = execMvnCommand(mvnArgs, path);
            } else {
               code = MavenCli.doMain(mvnArgs, myClassWorld);
            }

         }
         if (code != 0) {
            throw new Exception("Error executing: mvn " + Joiner.on(" ").join(mvnArgs) + " in " + path);
         }
      }
   }

   private boolean existsMvnCommand() {

      boolean result = false;
      try {
         ProcessBuilder pb = new ProcessBuilder("mvn", "-version");
         
         Process p = pb.start();
         p.waitFor(5000, TimeUnit.MILLISECONDS);
        result = true;
      } catch (Exception e) {
        
      }
      return result;
   }
   
   public boolean waitFor(Process p, long timeout, TimeUnit unit)
         throws InterruptedException
     {
         long startTime = System.nanoTime();
         long rem = unit.toNanos(timeout);

         do {
             try {
                 p.exitValue();
                 return true;
             } catch(IllegalThreadStateException ex) {
                 if (rem > 0)
                     Thread.sleep(
                         Math.min(TimeUnit.NANOSECONDS.toMillis(rem) + 1, 100));
             }
             rem = unit.toNanos(timeout) - (System.nanoTime() - startTime);
         } while (rem > 0);
         return false;
     }

   private int execMvnCommand(String[] mvnArgs, String path) throws Exception {
      path = pom.getBaseDirectory().getAbsolutePath();
      String[] command = new String[mvnArgs.length + 1];
      command[0] = "mvn";
      for (int i = 0; i < mvnArgs.length; i++) {
         command[i + 1] = mvnArgs[i];
      }
      Process p = Runtime.getRuntime().exec(command, null, new File(path));
      return p.waitFor();
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
                     isModule = module.getGroupId().equals(artifact.getGroupId())
                           && module.getArtifactId().equals(artifact.getArtifactId());
                  }
                  if (isModule) {
                     try {
                        module.compile();
                     } catch (Exception e) {
                        throw new ConfigurationException("Error compling the module " + module.getName(), e);
                     }
                  }

               }

            }

            if (pom.getDependencies() != null && !pom.getDependencies().isEmpty()) {
               MavenResolverSystemImpl mrs = (MavenResolverSystemImpl) Resolvers.use(MavenResolverSystem.class, cl);
               mrs.getMavenWorkingSession().useLegacyLocalRepository(true);

               MavenResolvedArtifact[] artifacts = mrs.loadPomFromFile(pomFile)
                     .importDependencies(ScopeType.COMPILE, ScopeType.TEST, ScopeType.PROVIDED, ScopeType.RUNTIME)
                     .resolve().withTransitivity().asResolvedArtifact();

               this.artifacts = Arrays.asList(artifacts);

            } else {
               this.artifacts = new LinkedList<MavenResolvedArtifact>();
            }
         }
         return artifacts;
      } else {
         throw new ConfigurationException("The pom.xml file at [" + pomFile.getAbsolutePath() + "] does not exists");
      }

   }

   private List<String> getSystemDeps() {
      List<String> systemDeps = new LinkedList<String>();
      Model pom = getModel(pomFile);
      List<Dependency> localDeps = pom.getDependencies();

      if (localDeps != null) {
         Iterator<Dependency> it = localDeps.iterator();

         while (it.hasNext()) {
            Dependency aux = it.next();
            if ("system".equals(aux.getScope())) {
               systemDeps.add(aux.getSystemPath());
            }
         }
      }
      return systemDeps;
   }

   public URLClassLoader resolveClassLoader() throws Exception {

      build();
      List<String> classPathEntries = new LinkedList<String>();
      classPathEntries.add("target/classes");
      classPathEntries.add("target/test-classes");

      String[] bootPath = System.getProperties().get("sun.boot.class.path").toString()
            .split(Character.toString(File.pathSeparatorChar));

      classPathEntries.addAll(getSystemDeps());
      List<MavenResolvedArtifact> artifacts = getArtifacts();

      if (artifacts != null) {
         URL[] classPath = new URL[artifacts.size() + classPathEntries.size() + bootPath.length];
         int i = 0;
         for (String lib : bootPath) {

            classPath[i] = new File(lib).toURI().toURL();

            i++;
         }
         for (String entry : classPathEntries) {
            try {

               classPath[i] = new File(entry).toURI().toURL();

            } catch (MalformedURLException e) {
               throw new ConfigurationException(
                     "Invalid URL for the classpath entry " + new File(entry).getAbsolutePath(), e.getCause());
            }
            i++;
         }
         for (MavenResolvedArtifact mra : artifacts) {
            try {
               classPath[i] = mra.asFile().toURI().toURL();

            } catch (MalformedURLException e) {
               throw new ConfigurationException("Invalid URL for the dependency " + mra.asFile().getAbsolutePath(),
                     e.getCause());
            }
            i++;
         }

         return new URLClassLoader(classPath) {

            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
               Class<?> result = null;
               try {
                  result = findClass(name);

               } catch (Throwable e) {

               }
               if (result != null) {
                  return result;
               }

               return super.loadClass(name, resolve);
            }

            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
               return loadClass(name, false);
            }
         };
      }
      return null;
   }
}
