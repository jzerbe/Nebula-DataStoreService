package nebuladss;

import java.util.ArrayList;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import contrib.AeSimpleSHA1;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Jason Zerbe
 */
public class HttpCmdClientTest implements ProgramConstants {

    String myTestNameSpace = "test-namespace";
    String myTestFileName = "";

    public HttpCmdClientTest() {
        Random aRandom = new Random();
        int aRandomInt = aRandom.nextInt(100); //0-99
        String aHashStr = "";
        try {
            aHashStr = AeSimpleSHA1.SHA1(NebulaDSS.getFormattedCurrentDate(kDateFormat_USA_Standard) + " + " + String.valueOf(aRandomInt));
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(HttpCmdClientTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(HttpCmdClientTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        myTestFileName = "test-filename-" + aHashStr;
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
        instance.notifyDown();
        assertEquals(expResult, result);
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
        instance.notifyDown();
        assertEquals(expResult, result);
    }

    /**
     * make sure the RTT is > 40 milliseconds for any HTTP server request/response
     */
    @Test
    public void testGetPeerHttpRttMillis() {
        System.out.println("getHttpRttMillis");
        HttpCmdClient instance = HttpCmdClient.getInstance(true);
        long result = instance.getPeerHttpRttMillis(instance.getMasterSeverUrlStr());
        System.out.println(result);
        assertEquals(true, (result > 40));
    }

    /**
     * just make sure low-level HTTP works
     */
    @Test
    public void testPutFile() {
        System.out.println("putFile");
        HttpCmdClient instance = HttpCmdClient.getInstance(true);
        boolean expResult = true;
        boolean result = instance.putFile(myTestNameSpace, myTestFileName);
        assertEquals(expResult, result);
    }

    /**
     * check to see if the previously created file exists
     */
    @Test
    public void testGetFile() {
        System.out.println("getFile");
        HttpCmdClient instance = HttpCmdClient.getInstance(true);
        instance.notifyUp();
        instance.putFile(myTestNameSpace, myTestFileName);
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            Logger.getLogger(HttpCmdClientTest.class.getName()).log(Level.WARNING, null, ex);
        }
        ArrayList<String> aReturnArrayList = instance.getFile(myTestNameSpace, myTestFileName);
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            Logger.getLogger(HttpCmdClientTest.class.getName()).log(Level.WARNING, null, ex);
        }
        instance.notifyDown();
        assertEquals(1, aReturnArrayList.size());
    }
}
