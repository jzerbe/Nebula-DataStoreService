/*
 * singleton class for interfacing with the master server
 */
package nebuladss;

import contrib.AeSimpleSHA1;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * @author Jason Zerbe
 */
public class MasterServer implements ProgramConstants {

    private static MasterServer ms_singleInstance = null;
    private Preferences ms_Preferences = null;
    private String ms_NodeUUID = null;
    private String ms_theServerUrlStr = kMasterServerBaseUrlStr;
    private int ms_HttpPortNumber = kWebAppDefaultPortInt;

    protected MasterServer() {
        ms_Preferences = Preferences.userNodeForPackage(getClass());
        ms_NodeUUID = ms_Preferences.get(kNodeUUIDKeyStr, ms_NodeUUID);
        if (ms_NodeUUID == null) {
            try {
                ms_NodeUUID = AeSimpleSHA1.SHA1(NebulaDSS.getFormattedCurrentDate(kDateFormat_USA_Standard) + " + " + ms_HttpPortNumber);
            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(MasterServer.class.getName()).log(Level.SEVERE, null, ex);
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(MasterServer.class.getName()).log(Level.SEVERE, null, ex);
            }
            ms_Preferences.put(kNodeUUIDKeyStr, ms_NodeUUID);
        }
    }

    public static MasterServer getInstance() {
        if (ms_singleInstance == null) {
            ms_singleInstance = new MasterServer();
        }
        return ms_singleInstance;
    }

    public String getUUID() {
        return ms_NodeUUID;
    }

    /**
     * tell the master server that this node has the certain file. if the file
     * is new then it needs to be timestamped to differentiated between the versions
     * @param theNameSpace String
     * @param theFileName String
     * @param theVersionNumber String
     * @return boolean - did contacting the master server work?
     */
    public boolean putFile(String theNameSpace, String theFileName, String theVersionNumber) {
        String aURLConnectionParamStr = "opt=put" + "&uuid=" + ms_NodeUUID
                + "&namespace=" + theNameSpace + "&filename=" + theFileName + "&version=";
        if (theVersionNumber.equals("new")) {
            aURLConnectionParamStr.concat(NebulaDSS.getFormattedCurrentDate(kDateFormat_precise_long));
        } else {
            aURLConnectionParamStr.concat(theVersionNumber);
        }
        return voidServerMethod(aURLConnectionParamStr);
    }

    /**
     * add this node to the master server's list of up nodes
     * @return boolean - was this node added properly?
     */
    public boolean addSelf() {
        String aURLConnectionParamStr = "opt=ping" + "&uuid=" + ms_NodeUUID
                + "&http=" + ms_HttpPortNumber;
        return voidServerMethod(aURLConnectionParamStr);
    }

    /**
     * remove this node from the list on the master server of available nodes
     * @return boolean - was this node successfully removed?
     */
    public boolean removeSelf() {
        String aURLConnectionParamStr = "opt=ping" + "&uuid=" + ms_NodeUUID
                + "&http=" + ms_HttpPortNumber
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
        String aURLConnectionParamStr = "opt=get" + "&uuid=" + ms_NodeUUID
                + "&filename=" + theFileName + "&namespace=" + theNameSpace
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

    /**
     * sets the port number that the HTTP interface is listening on
     * @param thePortNumber Integer
     */
    public void setHttpPortNumber(int thePortNumber) {
        ms_HttpPortNumber = thePortNumber;
    }
}
