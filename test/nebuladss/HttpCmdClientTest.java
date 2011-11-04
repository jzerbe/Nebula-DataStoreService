package nebuladss;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Jason Zerbe
 */
public class HttpCmdClientTest {

    public HttpCmdClientTest() {
    }

    /**
     * check to make sure lower level HTTP processing works
     */
    @Test
    public void testNotifyDown() {
        System.out.println("notifyDown");
        HttpCmdClient instance = HttpCmdClient.getInstance(true);
        boolean expResult = true;
        boolean result = instance.notifyDown();
        assertEquals(expResult, result);
    }

    /**
     * does not check result, checks to be sure low-level HTTP works
     */
    @Test
    public void testNotifyUp() {
        System.out.println("notifyUp");
        HttpCmdClient instance = HttpCmdClient.getInstance(true);
        boolean expResult = true;
        boolean result = instance.notifyUp();
        assertEquals(expResult, result);
        instance.notifyDown();
    }

    /**
     * check to see if a certain UUID is available, actually checks result
     */
    @Test
    public void testIsUUIDUp() {
        System.out.println("isUUIDUp");
        HttpCmdClient instance = HttpCmdClient.getInstance(true);
        instance.notifyUp();
        boolean expResult = true;
        boolean result = instance.isUUIDUp();
        assertEquals(expResult, result);
        instance.notifyDown();
    }
}
