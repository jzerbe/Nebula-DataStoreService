package nebuladss;

import nebuladss.Layer3Info.IpAddressType;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Jason Zerbe
 */
public class Layer3InfoTest {

    public Layer3InfoTest() {
    }

    /**
     * make sure function is able to return some IP address
     */
    @Test
    public void testGetValidIPAddress() {
        System.out.println("getValidIPAddress");
        IpAddressType theIpAddressType = IpAddressType.IPv4;
        Layer3Info instance = Layer3Info.getInstance(true);
        String result = instance.getValidIPAddress(theIpAddressType);
        assertEquals((result != null), true);
    }

    /**
     * just make sure function gets to point to return something valid
     */
    @Test
    public void testIsHostBehindNAT() {
        System.out.println("isHostBehindNAT");
        Layer3Info instance = Layer3Info.getInstance(true);
        boolean result = instance.isHostBehindNAT();
        assertEquals(result, (true || false));
        System.out.println(result);
    }
}
