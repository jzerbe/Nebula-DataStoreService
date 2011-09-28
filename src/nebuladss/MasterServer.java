/*
 * singleton class for interfacing with the master server: used in getting
 * boostrap nodes and pinging said master server over HTTP that this node
 * is up
 */
package nebuladss;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jason Zerbe
 */
public class MasterServer implements ProgramConstants {

    private static MasterServer ms_singleInstance = null;
    protected String ms_theServerUrlStr = kMasterServerBaseUrlStr;

    protected MasterServer() {
    }

    public static MasterServer getInstance() {
        if (ms_singleInstance == null) {
            ms_singleInstance = new MasterServer();
        }
        return ms_singleInstance;
    }

    /**
     * add this node to the master server's list of up nodes
     * @return boolean - was this node added properly?
     */
    public boolean addSelf() {
        String aURLConnectionParamStr = "opt=ping"
                + "&udpport=" + TomP2P.getInstance().getActualUDPPortInt()
                + "&tcpport=" + TomP2P.getInstance().getActualTCPPortInt();
        return voidServerMethod(aURLConnectionParamStr);
    }

    /**
     * remove this node from the list on the master server of available nodes
     * @return boolean - was this node successfully removed?
     */
    public boolean removeSelf() {
        String aURLConnectionParamStr = "opt=ping"
                + "&udpport=" + TomP2P.getInstance().getActualUDPPortInt()
                + "&tcpport=" + TomP2P.getInstance().getActualTCPPortInt()
                + "&remove=true";
        return voidServerMethod(aURLConnectionParamStr);
    }

    /**
     * internal method for doing a POST with what should be a void response
     * @param aURLConnectionParamStr String
     * @return boolean - was there a local or server side error?
     */
    protected boolean voidServerMethod(String aURLConnectionParamStr) {
        //create the HTTP buffered reader
        BufferedReader aBufferedReader = null;
        try {
            aBufferedReader = new BufferedReader(new InputStreamReader(getURLConnection(aURLConnectionParamStr).getInputStream()));
        } catch (IOException ex) {
            Logger.getLogger(MasterServer.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        //read from the HTTP stream
        ArrayList<String> returnArrayList = new ArrayList();
        try {
            String aString = null;
            while ((aString = aBufferedReader.readLine()) != null) {
                returnArrayList.add(aString);
            }
            if (returnArrayList.size() > 1) {
                return false;
            }
        } catch (IOException ex) {
            Logger.getLogger(MasterServer.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        //close up buffered HTTP stream reader
        try {
            aBufferedReader.close();
        } catch (IOException ex) {
            Logger.getLogger(MasterServer.class.getName()).log(Level.WARNING, null, ex);
        }

        return true;
    }

    /**
     * gets an ArrayList of possible bootstrap addresses to connect to
     * @return ArrayList<String>
     */
    public ArrayList<String> getBootstrapNodes() {
        //create the HTTP buffered reader
        BufferedReader aBufferedReader = null;
        try {
            aBufferedReader = new BufferedReader(new InputStreamReader(getURLConnection("opt=bootstrap").getInputStream()));
        } catch (IOException ex) {
            Logger.getLogger(MasterServer.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }

        //read from the HTTP stream
        ArrayList<String> returnArrayList = new ArrayList();
        try {
            String aString = null;
            while ((aString = aBufferedReader.readLine()) != null) {
                returnArrayList.add(aString);
            }
        } catch (IOException ex) {
            Logger.getLogger(MasterServer.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }

        //close up buffered HTTP stream reader
        try {
            aBufferedReader.close();
        } catch (IOException ex) {
            Logger.getLogger(MasterServer.class.getName()).log(Level.WARNING, null, ex);
        }

        return returnArrayList;
    }

    /**
     * helper method for construction a URLConnection
     * @param args String - everything after the ? in the URL
     * @return URLConnection
     */
    protected URLConnection getURLConnection(String args) {
        //create the resolvable url
        URL aUrl = null;
        try {
            aUrl = new URL(ms_theServerUrlStr + "?" + args);
        } catch (MalformedURLException ex) {
            Logger.getLogger(MasterServer.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }

        //make the connection to the url
        URLConnection aURLConnection = null;
        try {
            aURLConnection = aUrl.openConnection();
        } catch (IOException ex) {
            Logger.getLogger(MasterServer.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }

        //url connection is ready
        return aURLConnection;
    }

    /**
     * sets the master server URL to connect to
     * @param theServerUrlStr String
     */
    public void setMasterSeverUrlStr(String theServerUrlStr) {
        ms_theServerUrlStr = theServerUrlStr;
    }
}
