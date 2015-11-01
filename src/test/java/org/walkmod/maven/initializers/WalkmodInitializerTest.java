package org.walkmod.maven.initializers;

import java.io.File;

import junit.framework.Assert;

import org.junit.Test;
import org.walkmod.conf.providers.XMLConfigurationProvider;

public class WalkmodInitializerTest {

	@Test
	public void test() throws Exception {
		MavenInitializer initializer = new MavenInitializer();
		File testDir = new File("src/test/sample");
		File cfg = new File(testDir, "walkmod.xml");
		
		if(cfg.exists()){
			cfg.delete();
		}
		
		File apiModule = new File(testDir, "api");
		File cfgApi = new File(apiModule, "walkmod.xml");

		if (cfgApi.exists()) {
			cfgApi.delete();
		}

		File testModule = new File(testDir, "test");
		File cfgTest = new File(testModule, "walkmod.xml");

		if (cfgTest.exists()) {
			cfgTest.delete();
		}

		XMLConfigurationProvider provider = new XMLConfigurationProvider(cfg.getAbsolutePath(), false);
		provider.createConfig();
		initializer.execute(provider);

		Assert.assertTrue(cfg.exists());
		Assert.assertTrue(cfgApi.exists());
		Assert.assertTrue(cfgTest.exists());

		cfgApi.delete();
		cfgTest.delete();
		cfg.delete();
		

	}

}
