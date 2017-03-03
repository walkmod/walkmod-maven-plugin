package org.walkmod.maven.providers;

import java.io.File;

/**
 * Created by raquelpau on 3/3/17.
 */
public class MavenCommand {

    public static String getCommand(){
        return getCommand(System.getenv("M2_HOME"));
    }

    public static String getBinaryFileName(){
        String os = System.getProperty("os.name").toLowerCase();
        String command = "mvn";
        if (os.contains("win")) {
            command += ".cmd";
        }
        return command;
    }

    public static String getCommand(String mavenHome){
        if(mavenHome == null){
            return getBinaryFileName();
        }
        if(!mavenHome.endsWith(File.separator)){
            mavenHome+=File.separator;
        }
        return mavenHome+"bin"+ File.separator+getBinaryFileName();
    }
}
