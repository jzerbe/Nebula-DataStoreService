/*
 * singleton class for managing port-forwarding and detection with weupnp client
 */
package contrib;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import nebuladss.ProgramConstants;
import org.wetorrent.upnp.GatewayDevice;
import org.wetorrent.upnp.GatewayDiscover;
import org.wetorrent.upnp.PortMappingEntry;
import org.xml.sax.SAXException;

/**
 * @author Jason Zerbe
 * @see http://code.google.com/p/weupnp/wiki/HowTo
 */
public class weupnp implements ProgramConstants {

    private static weupnp wu_singleInstance = null;
    protected GatewayDevice wu_GatewayDevice = null;

    protected weupnp() {
        GatewayDiscover discover = new GatewayDiscover();
        try {
            discover.discover();
        } catch (SocketException ex) {
            Logger.getLogger(weupnp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnknownHostException ex) {
            Logger.getLogger(weupnp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(weupnp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(weupnp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(weupnp.class.getName()).log(Level.SEVERE, null, ex);
        }
        wu_GatewayDevice = discover.getValidGateway();
    }

    public static weupnp getInstance() {
        if (wu_singleInstance == null) {
            wu_singleInstance = new weupnp();
        }
        return wu_singleInstance;
    }

    /**
     * add a port mapping to the LAN gateway that supports UPnP. if there is no
     * UPnP device on the network then we forgo mapping and just listen on the
     * suggestion port number
     * @param thePortTypeStr String - "TCP" OR "UDP"
     * @param thePortNumberInt Integer - the port to open up
     * @param thePortDescription String - description of what we are opening up
     * @return Integer - the port number that was actually mapped
     * @throws IOException
     * @throws SAXException 
     */
    public int addPortMapping(String thePortTypeStr, int thePortNumberInt,
            String thePortDescription) throws IOException, SAXException {
        if (wu_GatewayDevice == null) { //no gateway device
            return thePortNumberInt;
        } else { //there is a UPnP gateway device
            InetAddress aLocalAddress = wu_GatewayDevice.getLocalAddress();
            PortMappingEntry aPortMappingEntry = new PortMappingEntry();
            if (wu_GatewayDevice.getSpecificPortMappingEntry(thePortNumberInt, thePortTypeStr, aPortMappingEntry)) {
                //port mapping is already taken, try again with a different port
                return addPortMapping(thePortTypeStr, (thePortNumberInt + kPortMappingRetryOffsetInt), thePortDescription);
            } else {
                if (wu_GatewayDevice.addPortMapping(thePortNumberInt, thePortNumberInt,
                        aLocalAddress.getHostAddress(), thePortTypeStr, thePortDescription)) {
                    //port mapping worked
                    return thePortNumberInt;
                } else {
                    //port mapping failed, try again with a different port
                    return addPortMapping(thePortTypeStr, (thePortNumberInt + kPortMappingRetryOffsetInt), thePortDescription);
                }
            }
        }
    }

    /**
     * remove a port mapping from the LAN gateway that supports UPnp. that is if
     * there is a UPnP gateway device to deal with
     * @param thePortTypeStr String - "TCP" OR "UDP"
     * @param thePortNumberInt Integer - the port to remove
     * @return Boolean - was the port mapping removed?
     * @throws IOException
     * @throws SAXException 
     */
    public boolean removePortMapping(String thePortTypeStr, int thePortNumberInt) throws IOException, SAXException {
        if (wu_GatewayDevice == null) { //no UPnP gateway device
            return true;
        } else { //need to teardown mapping from some UPnP gateway device
            return wu_GatewayDevice.deletePortMapping(thePortNumberInt, thePortTypeStr);
        }
    }
}
