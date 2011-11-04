/*
 * main class for application
 */
package nebuladss;

import contrib.JettyWebServer;
import contrib.weupnp;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
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
    private static String nd_RootPathStr = "default-fs-store";
    private static boolean nd_DebugOn = true;

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
        Runtime.getRuntime().addShutdownHook(new RunWhenShuttingDown());

        for (String currentArg : args) {
            if (currentArg.contains("-h")) {
                System.out.println(kUsageStr);
                System.exit(0);
            } else {
                setGlobalValues(currentArg);
            }
        } //done processing arguments

        //start local filesystem management
        FileSystemManager.getInstance(nd_DebugOn).setMaxAvailableMegaBytes(nd_MaxSizeMegaBytes);
        FileSystemManager.getInstance().setStorageRootPath(nd_RootPathStr);

        //start jetty with servlets that accept GET/POST for file management
        try {
            JettyWebServer.getInstance(nd_HttpPortInt).startServer();
        } catch (Exception ex) {
            Logger.getLogger(NebulaDSS.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        }

        //set up portmapping (if needed)
        if (Layer3Info.getInstance(nd_DebugOn).isHostBehindNAT()) {
            try {
                nd_HttpPortInt = weupnp.getInstance().addPortMapping("TCP", nd_HttpPortInt, kPortMappingDescStr);
            } catch (IOException ex) {
                Logger.getLogger(NebulaDSS.class.getName()).log(Level.SEVERE, null, ex);
                System.exit(1);
            } catch (SAXException ex) {
                Logger.getLogger(NebulaDSS.class.getName()).log(Level.SEVERE, null, ex);
                System.exit(1);
            }
        }

        //set master server URL
        HttpCmdClient.getInstance(nd_DebugOn).setMasterSeverUrlStr(nd_MasterServerUrlStr);
        //tell master server about HTTP server once ready (Jetty needs to be running before)
        HttpCmdClient.getInstance().notifyUp();

        //echo out what just happened
        System.out.println("NebulaDSS started with NodeUUID = " + HttpCmdClient.getInstance().getUUID() + "\n"
                + "--http-port=" + String.valueOf(nd_HttpPortInt) + "\n"
                + "--root-path=" + nd_RootPathStr + "\n"
                + "--max-mb=" + String.valueOf(nd_MaxSizeMegaBytes) + "\n");
    }

    /**
     * stop all relevant services in order of graceful shutdown needs
     */
    public static class RunWhenShuttingDown extends Thread {

        @Override
        public void run() {
            //1. remove node from master server - do not confuse other peers
            HttpCmdClient.getInstance().notifyDown();

            //2. remove portmapping (if needed) - make sure UPnP IGD does not overload
            if (Layer3Info.getInstance().isHostBehindNAT()) {
                try {
                    weupnp.getInstance().removePortMapping("TCP", nd_HttpPortInt);
                } catch (IOException ex) {
                    Logger.getLogger(NebulaDSS.class.getName()).log(Level.WARNING, null, ex);
                } catch (SAXException ex) {
                    Logger.getLogger(NebulaDSS.class.getName()).log(Level.WARNING, null, ex);
                }
            }

            //3. shutdown jetty -  does not really matter if aborted abruptly
            try {
                JettyWebServer.getInstance().stopServer();
            } catch (Exception ex) {
                Logger.getLogger(NebulaDSS.class.getName()).log(Level.WARNING, null, ex);
            }
        }
    }

    /**
     * get the current date with a certain format
     * @param theDateFormatStr String
     * @return String - the date as a string
     */
    public static String getFormattedCurrentDate(String theDateFormatStr) {
        GregorianCalendar aGregorianCalendar = new GregorianCalendar();
        SimpleDateFormat aSimpleDateFormat = new SimpleDateFormat(theDateFormatStr);
        return aSimpleDateFormat.format(aGregorianCalendar.getTime());
    }
}
