package org.walkmod.maven.providers;

import java.io.File;

import org.apache.maven.cli.MavenCli;
import org.junit.Assert;
import org.junit.Test;

public class MavenTest {

	@Test
	public void testResolveShouldRetrievePomDependencies() throws Throwable {
		File pom = new File("pom.xml");
		Assert.assertTrue(pom.exists());
		invokeMaven();
	}

	public void invokeMaven() throws Throwable {
		MavenCli cli = new MavenCli();
		cli.doMain(new String[] { "package" }, "/Users/rpau/walkmodhub/javalang", System.out, System.err);

	}

}
