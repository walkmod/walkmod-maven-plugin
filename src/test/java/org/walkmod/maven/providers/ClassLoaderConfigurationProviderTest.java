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
import java.util.List;

import org.jboss.shrinkwrap.resolver.api.maven.MavenResolvedArtifact;
import org.junit.Assert;
import org.junit.Test;
import org.walkmod.conf.entities.Configuration;
import org.walkmod.conf.entities.impl.ConfigurationImpl;

public class ClassLoaderConfigurationProviderTest {

   @Test
   public void testeable() {
      Assert.assertTrue(true);
   }

   @Test
   public void testResolveShouldRetrievePomDependencies() throws Exception {
      File pom = new File("pom.xml");
      Assert.assertTrue(pom.exists());
      MavenProject reader = new MavenProject(pom);
      List<MavenResolvedArtifact> dependencies = reader.getArtifacts();
      Assert.assertFalse(dependencies.isEmpty());
   }

   @Test
   public void testClassLoaderIsSet() {
      ClassLoaderConfigurationProvider reader = new ClassLoaderConfigurationProvider();
      Configuration conf = new ConfigurationImpl();
      conf.setClassLoader(Thread.currentThread().getContextClassLoader());
      reader.init(conf);
      reader.load();
      Assert.assertNotNull(conf.getParameters().get("classLoader"));
   }

   @Test
   public void testClassLoaderFromModuleProject() throws Exception {
      File pom = new File("src/test/sample/test/pom.xml");
      Assert.assertTrue(pom.exists());
      String aux = System.getProperty("user.dir");
      try {
         System.setProperty("user.dir", pom.getParentFile().getAbsolutePath());
         MavenProject reader = new MavenProject(new File("pom.xml"));
         reader.build();
         List<MavenResolvedArtifact> dependencies = reader.getArtifacts();
         Assert.assertFalse(dependencies.isEmpty());
      } finally {
         System.setProperty("user.dir", aux);

      }
   }

   @Test
   public void testClassLoaderFromProjectWithParentProject() throws Exception {
      File pom = new File("src/test/sample2/pom.xml");
      Assert.assertTrue(pom.exists());
      String aux = System.getProperty("user.dir");
      try {
         System.setProperty("user.dir", pom.getParentFile().getAbsolutePath());
         MavenProject reader = new MavenProject(new File("pom.xml"));
         reader.build();
         List<MavenResolvedArtifact> dependencies = reader.getArtifacts();
         Assert.assertFalse(dependencies.isEmpty());
      } finally {
         System.setProperty("user.dir", aux);

      }
   }

   @Test
   public void testAvoidRecursiveExecutions() throws Exception {
      File pom = new File("src/test/recursive");
      String aux = System.getProperty("user.dir");
      try {
         System.setProperty("user.dir", pom.getAbsolutePath());

         ClassLoaderConfigurationProvider reader = new ClassLoaderConfigurationProvider();
         Configuration conf = new ConfigurationImpl();
         conf.setClassLoader(Thread.currentThread().getContextClassLoader());
         reader.init(conf);
         reader.load();
         Assert.assertNotNull(conf.getParameters().get("classLoader"));
      } finally {
         System.setProperty("user.dir", aux);

      }
   }

}
