/*
 * main class for application
 */
package nebuladss;

import contrib.TomP2P;

/**
 * @author Jason Zerbe
 */
public class NebulaDSS implements ProgramConstants {

    private static int nd_DhtPortInt = kDhtDefaultPortInt;
    private static int nd_HttpPortInt = kWebAppDefaultPortInt;
    private static String nd_MasterServerUrlStr = kMasterServerBaseUrlStr;

    /**
     * help method for setting the global start values from the command line parameters
     * @param theCurrentArg String
     */
    private static void setGlobalValues(String theCurrentArg) {
        String[] currentArgArray = theCurrentArg.split(kArgumentSplit);
        String currentArgStr = currentArgArray[(currentArgArray.length - 1)];
        if ((currentArgStr != null) && (!currentArgStr.isEmpty())) {
            if (theCurrentArg.contains(kDhtArgStr)) {
                nd_DhtPortInt = Integer.valueOf(currentArgStr);
            } else if (theCurrentArg.contains(kHttpArgStr)) {
                nd_HttpPortInt = Integer.valueOf(currentArgStr);
            } else if (theCurrentArg.contains(kMasterServerArgStr)) {
                nd_MasterServerUrlStr = currentArgStr;
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
            } else if (currentArg.contains(kDhtArgStr)) {
                setGlobalValues(currentArg);
            } else if (currentArg.contains(kHttpArgStr)) {
                setGlobalValues(currentArg);
            } else if (currentArg.contains(kMasterServerArgStr)) {
                setGlobalValues(currentArg);
            }
        } //done processing arguments

        //get bootstrap nodes from webcache via nodemanger (singleton)
        MasterServer aMasterServer = MasterServer.getInstance();
        aMasterServer.setMasterSeverUrlStr(nd_MasterServerUrlStr);
        String[] someBootStrapNodes = aMasterServer.getBootstrapNodes();

        //start tomp2p with given bootstrap nodes
        TomP2P aTomP2P = TomP2P.getInstance();
        aTomP2P.setListenPort(nd_DhtPortInt);

        //start jetty with servlets that accept GET/POST for file management

        //wait for "Enter" to shutdown service
    }
}
