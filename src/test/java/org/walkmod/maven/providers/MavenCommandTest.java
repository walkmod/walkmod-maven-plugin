package org.walkmod.maven.providers;

import org.codehaus.groovy.tools.shell.Command;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by raquelpau on 3/3/17.
 */
public class MavenCommandTest {

    @Test
    public void whenM2HoMeIsNotDefinedReturnsPath(){
        String command = MavenCommand.getCommand(null);
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("win")) {
            Assert.assertEquals("mvn", command);
        }
    }

    @Test
    public void whenM2HoMeIsDefinedUsesItAsPath(){
        String command = MavenCommand.getCommand("/usr/local/Cellar/maven/3.3.9");
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("win")) {
            Assert.assertEquals("/usr/local/Cellar/maven/3.3.9/bin/mvn", command);
        }
    }
}
