package org.walkmod.maven.providers;

import java.io.File;
import java.util.List;

import org.junit.Test;
import org.walkmod.maven.providers.ClassLoaderConfigurationProvider;

import junit.framework.TestCase;

public class PomReaderTest extends TestCase {
	

    @Test
    public void testeable() {
        assertTrue(true);
    }

    @Test
    public void testResolveShouldRetrievePomDependencies() {
        File pom = new File("./src/test/resources/pom-test.xml");
        assertTrue(pom.exists());
        ClassLoaderConfigurationProvider reader = new ClassLoaderConfigurationProvider(pom);
        List dependencies = reader.resolve();
        assertFalse(dependencies.isEmpty());
    }
	

}

