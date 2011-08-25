/*
 * interface for storing various program wide constants
 */
package nebuladss;

/**
 * @author Jason Zerbe
 */
public interface ProgramConstants {

    //generic constants
    public static final String kUsageStr = "usage: java -jar nebuladss.jar";
    
    //jetty web server information
    public static final String kWebAppContextPathStr = "/";
    public static final String kWebAppDirStr = "../webapp"; //root is the directory or the Jetty calling class
}
