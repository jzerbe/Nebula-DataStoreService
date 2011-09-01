/*
 * abstraction level for working with the TomP2P multi-map library.
 * TomP2P is a DMM - distributed multi-map for mapping keys to multiple values
 * with its underpinnings based almost entirely on the Kademlia spec
 */
package contrib;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import nebuladss.ProgramConstants;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.p2p.Peer;
import net.tomp2p.peers.Number160;
import org.xml.sax.SAXException;

/**
 * @author Jason Zerbe
 * @see http://tomp2p.net/doc/quick/ (quick setup)
 */
public class TomP2P implements ProgramConstants {

    private static TomP2P tp_singleInstance = null;
    private Peer tp_Peer = null;
    private int tp_ListenPortInt = kDhtDefaultPortInt;
    private int tc_ActualTCPPortInt = 0;
    private int tc_ActualUDPPortInt = 0;

    protected TomP2P() {
        Random rnd = new Random();
        tp_Peer = new Peer(new Number160(rnd));
    }

    public static TomP2P getInstance() {
        if (tp_singleInstance == null) {
            tp_singleInstance = new TomP2P();
        }
        return tp_singleInstance;
    }

    /**
     * connect to the TomP2P DHT network
     */
    public void start() {
        startListen();
        HashMap<String, Integer> someBootStrapNodes = null; //TODO: get boostrap nodes from webservice
        bootStrap(someBootStrapNodes);
        //TODO: ping webservice that this node is in the network
    }

    /**
     * stop the TomP2P DHT network connection
     */
    public void stop() {
        //TODO: ping webservice that this node is going down
        stopListen();
    }

    /**
     * set up the UDP and TCP listen sockets based on the port number suggestion.
     * keep incrementing from said suggestion if we are unable to setup the UPnP
     * port mapping. finally start the listening sockets
     * @return boolean - was this node able to start listening to the external network?
     */
    protected boolean startListen() {
        //setup the UPnP mapping for the udp port
        try {
            tc_ActualUDPPortInt = weupnp.getInstance().addPortMapping("UDP", tp_ListenPortInt, kPortMappingDescStr);
        } catch (IOException ex) {
            Logger.getLogger(TomP2P.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (SAXException ex) {
            Logger.getLogger(TomP2P.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        //do the TCP UPnP map
        try {
            tc_ActualTCPPortInt = weupnp.getInstance().addPortMapping("TCP", tp_ListenPortInt, kPortMappingDescStr);
        } catch (IOException ex) {
            Logger.getLogger(TomP2P.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (SAXException ex) {
            Logger.getLogger(TomP2P.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        //start the DHT listener
        try {
            tp_Peer.listen(tc_ActualUDPPortInt, tc_ActualTCPPortInt);
        } catch (Exception ex) {
            Logger.getLogger(TomP2P.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        //everything worked
        return true;
    }

    /**
     * tear down the UPnP gateway device mappings if they exist and
     * said device exists, then shutdown the TomP2P service
     * @return boolean - did everything work properly?
     */
    protected boolean stopListen() {
        //stop the DHT listener
        tp_Peer.shutdown();

        //tear down the UDP port mapping on the UPnP gateway device if it exists
        try {
            weupnp.getInstance().removePortMapping("UDP", tp_ListenPortInt);
        } catch (IOException ex) {
            Logger.getLogger(TomP2P.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (SAXException ex) {
            Logger.getLogger(TomP2P.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        //remove the TCP port mapping if it exists
        try {
            weupnp.getInstance().removePortMapping("TCP", tp_ListenPortInt);
        } catch (IOException ex) {
            Logger.getLogger(TomP2P.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (SAXException ex) {
            Logger.getLogger(TomP2P.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        //everything worked
        return true;
    }

    /**
     * get this local node into the existing TomP2P network
     * @param theBootStrapNodes HashMap<String, Integer>
     * @return boolean - were we able to bootstrap into the network?
     */
    protected boolean bootStrap(HashMap<String, Integer> theBootStrapNodes) {
        for (String aNodeAdress : theBootStrapNodes.keySet()) {
            InetSocketAddress aISA = new InetSocketAddress(aNodeAdress, theBootStrapNodes.get(aNodeAdress));
            FutureBootstrap aFutureBootstrap = tp_Peer.bootstrap(aISA); //pings the UDP socket of aISA
            aFutureBootstrap.awaitUninterruptibly();
            if (aFutureBootstrap.isSuccess()) {
                return true; //just need to boostrap with one node, more added automatically (see Kademlia spec)
            }
        }
        return false;
    }

    /**
     * set the suggested port to listen on. this may not be the port that the
     * UDP and TCP sockets end up listening on, depending on if said ports are
     * open to be forwarded through the UPnP gateway device (if there is one)
     * @param thePortInt Integer - the port number suggestion
     */
    public void setListenPort(int thePortInt) {
        tp_ListenPortInt = thePortInt;
    }
}
