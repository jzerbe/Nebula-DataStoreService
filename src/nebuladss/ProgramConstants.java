/*
 * interface for storing various program wide constants
 */
package nebuladss;

/**
 * @author Jason Zerbe
 */
public interface ProgramConstants {

    //argument strings
    public static final String kDhtArgStr = "--dht-port";
    public static final String kHttpArgStr = "--http-port";
    public static final String kMasterServerArgStr = "--master-server";
    //generic constants
    public static final String kUsageStr = "usage: java -jar nebuladss.jar "
            + kDhtArgStr + "=[port > 2000 (default=2010)] "
            + kHttpArgStr + "=[port > 2000 (default=2020)] "
            + kMasterServerArgStr + "=[url string of master server]";
    public static final String kArgumentSplit = "=";
    //dht constansts/setup
    public static final String kPortMappingDescStr = "port mapping for NebulaDSS DHT";
    public static final int kPortMappingRetryOffsetInt = 11;
    public static final int kDhtDefaultPortInt = 2010;
    //jetty web server information
    public static final String kWebAppContextPathStr = "/";
    public static final String kWebAppDirStr = "../webapp"; //root is the directory or the Jetty calling class
    public static final int kWebAppDefaultPortInt = 2020;
    //master server stuffs
    public static final String kMasterServerBaseUrlStr = "http://www-users.cs.umn.edu/~zerbe/nebula_dss/webcache/";
    //dss operation types used in HTTP operation param
    public static enum enumOperationType {GET, PUT, DELETE};
}
