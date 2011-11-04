/*
 * interface for storing various program wide constants
 */
package nebuladss;

/**
 * @author Jason Zerbe
 */
public interface ProgramConstants {

    //terminal I/O strings
    public static final String kArgumentSplit = "=";
    public static final String kRootStorageArgStr = "--root-path";
    public static final String kMaxMegaBytesUsageArgStr = "--max-mb";
    public static final String kHttpArgStr = "--http-port";
    public static final String kMasterServerArgStr = "--master-server";
    public static final String kUsageStr = "usage: java -jar nebuladss.jar "
            + kHttpArgStr + "=[port number (default=2020)] "
            + kMasterServerArgStr + "=[url string of master server]"
            + kRootStorageArgStr + "=[root storage directory path]"
            + kMaxMegaBytesUsageArgStr + "=[maximum filesystem usage in megabytes (soft quota)]";
    //port mapping for HTTP interface
    public static final String kPortMappingDescStr = "port mapping for NebulaDSS HTTP";
    public static final int kPortMappingRetryOffsetInt = 11;
    public static final int kWebAppDefaultPortInt = 2020;
    //jetty web server information
    public static final String kWebAppContextPathStr = "/";
    public static final String kWebAppDirStr = "../webapp"; //root is the directory or the Jetty calling class
    //master server stuffs
    public static final String kMasterServerBaseUrlStr = "http://www-users.cs.umn.edu/~zerbe/nebula_dss/webcache/";
    public static final String kMasterServerReturnStringSplitStr = "=";
    //storage constants
    public static final String kNodeUUIDKeyStr = "NodeUUID";
    public static final int kStorageDefaultMaxSizeMegaBytes = 512; //default to half-gig available
    //date and time format strings --> http://download.oracle.com/javase/1,5.0/docs/api/java/text/SimpleDateFormat.html
    public static final String kDateFormat_USA_Standard = "yyyy-MM-dd HH:mm:ss";
    public static final String kDateFormat_24_hr_precise = "HH:mm:ss:SSS";
    public static final String kDateFormat_precise_long = "yyyyMMddHHmmssSSS";
    //task constants
    public static final int kDefaultTaskTimerSeconds = 180;
}
