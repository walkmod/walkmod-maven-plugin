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
import org.junit.Test;
import org.walkmod.conf.entities.Configuration;
import org.walkmod.conf.entities.impl.ConfigurationImpl;
import org.walkmod.maven.providers.ClassLoaderConfigurationProvider;

import junit.framework.TestCase;

public class ClassLoaderConfigurationProviderTest extends TestCase {
	

    @Test
    public void testeable() {
        assertTrue(true);
    }

    @Test
    public void testResolveShouldRetrievePomDependencies() throws Exception {
        File pom = new File("pom.xml");
        assertTrue(pom.exists());
        ClassLoaderConfigurationProvider reader = new ClassLoaderConfigurationProvider(pom);
        List<MavenResolvedArtifact> dependencies = reader.getArtifacts();
        assertFalse(dependencies.isEmpty());
    }
	
    @Test
    public void testClassLoaderIsSet(){
    	ClassLoaderConfigurationProvider reader = new ClassLoaderConfigurationProvider();
    	Configuration conf = new ConfigurationImpl();
    	reader.init(conf);
    	reader.load();
    	assertNotNull(conf.getParameters().get("classLoader"));
    }
    
}

