/*
 * main class for application
 */
package nebuladss;

import contrib.JettyWebServer;
import contrib.weupnp;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xml.sax.SAXException;

/**
 * @author Jason Zerbe
 */
public class NebulaDSS implements ProgramConstants {
    
    private static int nd_HttpPortInt = kWebAppDefaultPortInt;
    private static String nd_MasterServerUrlStr = kMasterServerBaseUrlStr;
    private static int nd_MaxSizeMegaBytes = kStorageDefaultMaxSizeMegaBytes;
    private static String nd_RootPathStr = "";

    /**
     * help method for setting the global start values from the command line parameters
     * @param theCurrentArg String
     */
    private static void setGlobalValues(String theCurrentArg) {
        String[] currentArgArray = theCurrentArg.split(kArgumentSplit);
        String currentArgStr = currentArgArray[(currentArgArray.length - 1)];
        if ((currentArgStr != null) && (!currentArgStr.isEmpty())) {
            if (theCurrentArg.contains(kHttpArgStr)) {
                nd_HttpPortInt = Integer.valueOf(currentArgStr);
            } else if (theCurrentArg.contains(kMasterServerArgStr)) {
                nd_MasterServerUrlStr = currentArgStr;
            } else if (theCurrentArg.contains(kMaxMegaBytesUsageArgStr)) {
                nd_MaxSizeMegaBytes = Integer.valueOf(currentArgStr);
            } else if (theCurrentArg.contains(kRootStorageArgStr)) {
                nd_RootPathStr = currentArgStr;
            }
        }
    }

    /**
     * the application execution start point
     * @param args String[]
     */
    public static void main(String[] args) {
        for (String currentArg : args) {
            if (currentArg.contains("-h")) {
                System.out.println(kUsageStr);
                System.exit(0);
            } else {
                setGlobalValues(currentArg);
            }
        } //done processing arguments

        //set bootstrap master server URL
        MasterServer aMasterServer = MasterServer.getInstance();
        aMasterServer.setMasterSeverUrlStr(nd_MasterServerUrlStr);

        //start local filesystem management
        FileSystemManager.getInstance().setMaxAvailableMegaBytes(nd_MaxSizeMegaBytes);
        FileSystemManager.getInstance().setStorageRootPath(nd_RootPathStr);

        //set up portmapping (if needed)
        weupnp aweupnp = weupnp.getInstance();
        try {
            nd_HttpPortInt = aweupnp.addPortMapping("TCP", nd_HttpPortInt, kPortMappingDescStr);
        } catch (IOException ex) {
            Logger.getLogger(NebulaDSS.class.getName()).log(Level.WARNING, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(NebulaDSS.class.getName()).log(Level.WARNING, null, ex);
        }

        //start jetty with servlets that accept GET/POST for file management
        JettyWebServer aJettyWebServer = JettyWebServer.getInstance(nd_HttpPortInt);
        aJettyWebServer.startServer();

        //tell master server about self - HTTP port
        aMasterServer.addSelf(nd_HttpPortInt);

        //open up control console
        //for more: http://www.javapractices.com/topic/TopicAction.do?Id=79
    }
}
