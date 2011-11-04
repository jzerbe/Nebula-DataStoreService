package contrib;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Jason Zerbe
 */
public class JettyWebServerTest {

    private int aTestServerPortInt = 8888;

    public JettyWebServerTest() {
    }

    /**
     * Test of getServerPortInt method, of class JettyWebServer.
     */
    @Test
    public void testGetServerPortInt() {
        System.out.println("getServerPortInt");
        JettyWebServer instance = JettyWebServer.getInstance(aTestServerPortInt);
        int result = instance.getServerPortInt();
        assertEquals(aTestServerPortInt, result);
    }

    /**
     * Test of startServer method, of class JettyWebServer.
     */
    @Test
    public void testStartServer() throws Exception {
        System.out.println("startServer");
        JettyWebServer instance = JettyWebServer.getInstance(aTestServerPortInt);
        instance.startServer();
        assertEquals(instance.isRunning(), true);
        instance.stopServer();
    }

    /**
     * Test of stopServer method, of class JettyWebServer.
     */
    @Test
    public void testStopServer() throws Exception {
        System.out.println("stopServer");
        JettyWebServer instance = JettyWebServer.getInstance(aTestServerPortInt);
        instance.startServer();
        instance.stopServer();
        assertEquals(instance.isRunning(), false);
    }
}
