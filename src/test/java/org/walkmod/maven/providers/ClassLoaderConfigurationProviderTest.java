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
    public void testResolveShouldRetrievePomDependencies() {
        File pom = new File("pom.xml");
        assertTrue(pom.exists());
        ClassLoaderConfigurationProvider reader = new ClassLoaderConfigurationProvider(pom);
        List<MavenResolvedArtifact> dependencies = reader.resolve();
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

