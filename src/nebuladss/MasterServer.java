/*
 * singleton class for interfacing with the master server: used in getting
 * boostrap nodes and pinging said master server over HTTP that this node
 * is up
 */
package nebuladss;

/**
 * @author Jason Zerbe
 */
public class MasterServer {

    private static MasterServer ms_singleInstance = null;
    private String ms_theServerUrlStr = null;

    protected MasterServer() {
    }

    public static MasterServer getInstance() {
        if (ms_singleInstance == null) {
            ms_singleInstance = new MasterServer();
        }
        return ms_singleInstance;
    }

    public String[] getBootstrapNodes() {
        return null;
    }

    public void setMasterSeverUrlStr(String theServerUrlStr) {
        ms_theServerUrlStr = theServerUrlStr;
    }
}
