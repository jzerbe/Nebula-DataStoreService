package contrib;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Jason Zerbe
 */
public class AeSimpleSHA1Test {

    public AeSimpleSHA1Test() {
    }

    /**
     * Test of SHA1 method, of class AeSimpleSHA1.
     */
    @Test
    public void testSHA1() throws Exception {
        System.out.println("SHA1");
        String text = "this is a test message!";
        String expResult = "d6c6e8cfb4318a2384010451274e4481428b1326";
        String result = AeSimpleSHA1.SHA1(text);
        assertEquals(expResult, result);
    }
}
