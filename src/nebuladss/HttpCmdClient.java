/*
 * singleton class for issuing HTTP GET requests to master server and other nodes
 */
package nebuladss;

import contrib.AeSimpleSHA1;
import contrib.JettyWebServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * @author Jason Zerbe
 */
public class HttpCmdClient implements ProgramConstants {

    private static HttpCmdClient hcc_singleInstance = null;
    private boolean hcc_DebugOn = false;
    private Preferences hcc_Preferences = null;
    private String hcc_NodeUUID = null;
    private String hcc_theMasterServerUrlStr = kMasterServerBaseUrlStr;

    protected HttpCmdClient(boolean theDebugOn) {
        hcc_DebugOn = theDebugOn;
        hcc_Preferences = Preferences.userNodeForPackage(getClass());
        hcc_NodeUUID = hcc_Preferences.get(kNodeUUIDKeyStr, hcc_NodeUUID);
        if (hcc_NodeUUID == null) {
            Random aRandom = new Random();
            int aRandomInt = aRandom.nextInt(100); //0-99
            try {
                hcc_NodeUUID = AeSimpleSHA1.SHA1(NebulaDSS.getFormattedCurrentDate(kDateFormat_USA_Standard) + " + " + String.valueOf(aRandomInt));
            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(HttpCmdClient.class.getName()).log(Level.SEVERE, null, ex);
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(HttpCmdClient.class.getName()).log(Level.SEVERE, null, ex);
            }
            hcc_Preferences.put(kNodeUUIDKeyStr, hcc_NodeUUID);
        }
    }

    public static HttpCmdClient getInstance() {
        if (hcc_singleInstance == null) {
            hcc_singleInstance = new HttpCmdClient(false);
        }
        return hcc_singleInstance;
    }

    public static HttpCmdClient getInstance(boolean theDebugOn) {
        if (hcc_singleInstance == null) {
            hcc_singleInstance = new HttpCmdClient(theDebugOn);
        }
        return hcc_singleInstance;
    }

    public void setMasterSeverUrlStr(String theServerUrlStr) {
        hcc_theMasterServerUrlStr = theServerUrlStr;
    }

    public String getMasterSeverUrlStr() {
        return hcc_theMasterServerUrlStr;
    }

    public String getUUID() {
        return hcc_NodeUUID;
    }

    /**
     * tell the master server that this node has the certain file. if the file
     * is new then it needs to be timestamped to differentiated between the versions
     * @param theNameSpace String
     * @param theFileName String
     * @return boolean - did contacting the master server work?
     */
    public boolean putFile(String theNameSpace, String theFileName) {
        String aURLConnectionParamStr = "opt=put" + "&uuid=" + getUUID()
                + "&namespace=" + theNameSpace + "&filename=" + theFileName;
        return voidServerMethod(hcc_theMasterServerUrlStr, aURLConnectionParamStr);
    }

    /**
     * get an ArrayList of URL strings
     * @param theNameSpace String
     * @param theFileName String
     * @return ArrayList<String>
     */
    public ArrayList<String> getFile(String theNameSpace, String theFileName) {
        String aURLConnectionParamStr = "opt=get" + "&namespace=" + theNameSpace
                + "&filename=" + theFileName;
        ArrayList<String> returnArrayList = returnServerMethod(hcc_theMasterServerUrlStr, aURLConnectionParamStr);

        //output debug
        if (hcc_DebugOn) {
            if (returnArrayList != null) {
                for (int i = 0; i < returnArrayList.size(); i++) {
                    System.out.println(this.getClass().getName() + " - getFile - " + returnArrayList.get(i));
                }
            }
        }

        return returnArrayList;
    }

    /**
     * HTTP request/response time in Milliseconds - elapsed time of operation
     * this is used for gauging RTT between this node and random HTTP server
     * @param theUrlBase String
     * @return long - RTT in Milliseconds of this operation
     */
    public long getPeerHttpRttMillis(String theUrlBase) {
        long aStartTimeMillis = System.currentTimeMillis();
        String aUrlArgs = "opt=latency";
        returnServerMethod(theUrlBase, aUrlArgs);
        long aElapsedTimeMillis = System.currentTimeMillis() - aStartTimeMillis;
        return aElapsedTimeMillis;
    }

    /**
     * notify master server (calling) node is up, empty response = no errors
     * @return boolean - did server respond with empty page?
     */
    public boolean notifyUp() {
        String aURLConnectionParamStr = "opt=ping" + "&uuid=" + getUUID()
                + "&http=" + String.valueOf(JettyWebServer.getInstance().getServerPortInt());
        return voidServerMethod(hcc_theMasterServerUrlStr, aURLConnectionParamStr);
    }

    /**
     * check to see if UUID is online according to master server
     * @return boolean - does master server think UUID online?
     */
    public boolean isUUIDUp() {
        String aOperationCheckOnlineUUID = "online-uuid";
        String aURLConnectionParamStr = "opt=" + aOperationCheckOnlineUUID
                + "&uuid=" + getUUID();
        ArrayList<String> returnArrayList = returnServerMethod(hcc_theMasterServerUrlStr, aURLConnectionParamStr);
        if (returnArrayList == null) {
            return false;
        }

        //output debug
        if (hcc_DebugOn) {
            for (int i = 0; i < returnArrayList.size(); i++) {
                System.out.println(this.getClass().getName() + " - isUUIDUp - " + returnArrayList.get(i));
            }
        }

        //does the master server think the UUID is up?
        for (int i = 0; i < returnArrayList.size(); i++) {
            if (returnArrayList.get(i).contains("online=true")) {
                return true;
            }
        }
        return false;
    }

    /**
     * notify master server node is down/offline, empty response = no errors
     * @return boolean - did server respond with empty page?
     */
    public boolean notifyDown() {
        String aURLConnectionParamStr = "opt=ping" + "&uuid=" + getUUID()
                + "&http=" + String.valueOf(JettyWebServer.getInstance().getServerPortInt())
                + "&down=true";
        return voidServerMethod(hcc_theMasterServerUrlStr, aURLConnectionParamStr);
    }

    /**
     * HTTP GET should have a void response - empty single line returned
     * @param theUrlBase String - part before "?"
     * @param theUrlArgs String - part after "?"
     * @return boolean - was there a local or server side error?
     */
    protected boolean voidServerMethod(String theUrlBase, String theUrlArgs) {
        ArrayList<String> returnArrayList = returnServerMethod(theUrlBase, theUrlArgs);
        if ((returnArrayList != null) && (returnArrayList.size() > 1)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * internal method for doing a GET with response retrieval
     * @param theUrlBase String - part before "?"
     * @param theUrlArgs String - part after "?"
     * @return ArrayList<String>
     */
    protected ArrayList<String> returnServerMethod(String theUrlBase, String theUrlArgs) {
        //be able to function w/o master server
        if ((theUrlBase == null) || (theUrlBase.contains(kMasterServerNoUrlStr))) {
            return null;
        }

        //create the HTTP buffered reader
        BufferedReader aBufferedReader = null;
        try {
            aBufferedReader = new BufferedReader(new InputStreamReader(getURLConnection(theUrlBase, theUrlArgs).getInputStream()));
        } catch (IOException ex) {
            Logger.getLogger(HttpCmdClient.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }

        //read from the HTTP stream
        ArrayList<String> returnArrayList = new ArrayList<String>();
        try {
            String aString = null;
            while ((aString = aBufferedReader.readLine()) != null) {
                if (!aString.equals("")) {
                    returnArrayList.add(aString);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(HttpCmdClient.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }

        //close up buffered HTTP stream reader
        try {
            aBufferedReader.close();
        } catch (IOException ex) {
            Logger.getLogger(HttpCmdClient.class.getName()).log(Level.WARNING, null, ex);
        }

        return returnArrayList;
    }

    /**
     * helper method for constructing a URLConnection
     * @param theUrlBase String - part before "?"
     * @param theUrlArgs String - part after "?"
     * @return URLConnection
     */
    protected URLConnection getURLConnection(String theUrlBase, String theUrlArgs) {
        //create the resolvable url
        URL aUrl = null;
        try {
            aUrl = new URL(theUrlBase + "?" + theUrlArgs);
        } catch (MalformedURLException ex) {
            Logger.getLogger(HttpCmdClient.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }

        //output debug
        if (hcc_DebugOn) {
            System.out.println(this.getClass().getName() + " - URL Connection - " + aUrl.toString());
        }

        //make the connection to the url
        URLConnection aURLConnection = null;
        try {
            aURLConnection = aUrl.openConnection();
        } catch (IOException ex) {
            Logger.getLogger(HttpCmdClient.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }

        //url connection is ready
        return aURLConnection;
    }
}
