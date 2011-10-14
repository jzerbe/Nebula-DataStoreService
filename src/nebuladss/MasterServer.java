/*
 * singleton class for interfacing with the master server: used in  pinging 
 * master server over HTTP that this node is up
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
    private String ms_theServerUrlStr = kMasterServerBaseUrlStr;

    protected MasterServer() {
    }

    public static MasterServer getInstance() {
        if (ms_singleInstance == null) {
            ms_singleInstance = new MasterServer();
        }
        return ms_singleInstance;
    }

    /**
     * tell the master server that this node has the certain file
     * @param theNameSpace String
     * @param theFileName String
     * @return boolean - did contacting the master server work?
     */
    public boolean putFile(String theNameSpace, String theFileName) {
        String aURLConnectionParamStr = "opt=putfile"
                + "&namespace=" + theNameSpace + "&filename=" + theFileName;
        return voidServerMethod(aURLConnectionParamStr);
    }

    /**
     * add this node to the master server's list of up nodes
     * @param theHttpPortNumber Integer
     * @return boolean - was this node added properly?
     */
    public boolean addSelf(int theHttpPortNumber) {
        String aURLConnectionParamStr = "opt=ping"
                + "&http=" + theHttpPortNumber;
        return voidServerMethod(aURLConnectionParamStr);
    }

    /**
     * remove this node from the list on the master server of available nodes
     * @param theHttpPortNumber Integer
     * @return boolean - was this node successfully removed?
     */
    public boolean removeSelf(int theHttpPortNumber) {
        String aURLConnectionParamStr = "opt=ping"
                + "&http=" + theHttpPortNumber
                + "&remove=true";
        return voidServerMethod(aURLConnectionParamStr);
    }

    /**
     * internal method for doing a GET with what should be a void response
     * @param aURLConnectionParamStr String
     * @return boolean - was there a local or server side error?
     */
    protected boolean voidServerMethod(String aURLConnectionParamStr) {
        ArrayList<String> returnArrayList = returnServerMethod(aURLConnectionParamStr);
        if ((returnArrayList != null) && (returnArrayList.size() > 1)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * retrieves a list of file URL locations from the master server in
     * order of latency(1) and bandwidth(2) descending
     * @param theNameSpace String
     * @param theFileName String
     * @param theMaxMillisecondLatency Integer
     * @param theMinMbpsBandwidth Integer
     * @return ArrayList<String> - the list of URL locations
     */
    public ArrayList<String> getFileUrlList(String theNameSpace, String theFileName,
            int theMaxMillisecondLatency, int theMinMbpsBandwidth) {
        String aURLConnectionParamStr = "opt=get" + "&filename=" + theFileName
                + "&namespace=" + theNameSpace
                + "&latency_max=" + String.valueOf(theMaxMillisecondLatency)
                + "&bandwidth_min=" + String.valueOf(theMinMbpsBandwidth);
        ArrayList<String> returnArrayList = returnServerMethod(aURLConnectionParamStr);
        return returnArrayList;
    }

    /**
     * internal method for doing a GET with response retrieval
     * @param aURLConnectionParamStr String
     * @return ArrayList<String>
     */
    protected ArrayList<String> returnServerMethod(String aURLConnectionParamStr) {
        //create the HTTP buffered reader
        BufferedReader aBufferedReader = null;
        try {
            aBufferedReader = new BufferedReader(new InputStreamReader(getURLConnection(aURLConnectionParamStr).getInputStream()));
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
