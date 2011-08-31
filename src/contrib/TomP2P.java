/*
 * abstraction level for working with the TomP2P DHT library
 */
package contrib;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import nebuladss.ProgramConstants;
import net.tomp2p.futures.BaseFutureAdapter;
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

        //make sure this peer leaves the DHT properly
        //TODO: shutdownhook probably does not leave enough time, put somewhere else
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                tp_Peer.shutdown();
            }
        });
    }

    public static TomP2P getInstance() {
        if (tp_singleInstance == null) {
            tp_singleInstance = new TomP2P();
        }
        return tp_singleInstance;
    }

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

    protected void bootStrap(HashMap<String, Integer> theBootStrapNodes) {
        for (String aNodeAdress : theBootStrapNodes.keySet()) {
            InetSocketAddress aISA = new InetSocketAddress(aNodeAdress, theBootStrapNodes.get(aNodeAdress));
            FutureBootstrap aFutureBootstrap = tp_Peer.bootstrap(aISA);
            aFutureBootstrap.addListener(new BaseFutureAdapter<FutureBootstrap>() {

                public void operationComplete(FutureBootstrap f) throws Exception {
                    if (f.isSuccess()) {
                    }
                }
            });
        }
    }

    public void setListenPort(int thePortInt) {
        tp_ListenPortInt = thePortInt;
    }
}
