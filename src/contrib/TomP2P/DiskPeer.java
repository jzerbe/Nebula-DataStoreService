/*
 * class to extend TomP2P library Peer class to allow for disk storage
 */
package contrib.TomP2P;

import net.tomp2p.p2p.Peer;
import net.tomp2p.peers.Number160;

/**
 * @author Jason Zerbe
 */
public class DiskPeer extends Peer {

    public DiskPeer(final Number160 theNodeId) {
        super(theNodeId);
    }
}
