package contrib;

import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Jason Zerbe
 */
public class weupnpTest {

    int aMin = 2020;
    int aMax = 4040;
    Random aRandom;
    int aRandomPortNum = 0;

    public weupnpTest() {
        aRandomPortNum = aRandom.nextInt(aMax - aMin + 1) + aMin;
    }

    /**
     * Test of addPortMapping method, of class weupnp.
     */
    @Test
    public void testAddPortMapping() throws Exception {
        System.out.println("addPortMapping");
        String thePortTypeStr = "TCP";
        String thePortDescription = "JUnit port mapping test";
        weupnp instance = weupnp.getInstance();
        int expResult = aRandomPortNum;
        aRandomPortNum = instance.addPortMapping(thePortTypeStr, aRandomPortNum, thePortDescription);
        assertEquals((aRandomPortNum >= expResult), true);
        System.out.println(String.valueOf(aRandomPortNum) + " " + thePortTypeStr);
    }

    /**
     * Test of removePortMapping method, of class weupnp.
     */
    @Test
    public void testRemovePortMapping() throws Exception {
        System.out.println("removePortMapping");
        String thePortTypeStr = "TCP";
        weupnp instance = weupnp.getInstance();
        boolean result = instance.removePortMapping(thePortTypeStr, aRandomPortNum);
        assertEquals(result, true);
        System.out.println(String.valueOf(aRandomPortNum) + " " + thePortTypeStr);
    }
}
