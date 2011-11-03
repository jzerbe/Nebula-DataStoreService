package contrib;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Jason Zerbe
 */
public class AeSimpleSHA1Test {

    public AeSimpleSHA1Test() {
    }

    @Test
    public void testSHA1_1() throws Exception {
        System.out.println("SHA1 - test hashing 1");
        String text = "this is a test message!";
        String expResult = "d6c6e8cfb4318a2384010451274e4481428b1326";
        String result = AeSimpleSHA1.SHA1(text);
        assertEquals(expResult, result);
    }

    @Test
    public void testSHA1_2() throws Exception {
        System.out.println("SHA1 - test hashing 2");
        String text = "this is another test message 6674";
        String expResult = "30c925ae7fc8bff36d28f4c2b8949976e00e45a2";
        String result = AeSimpleSHA1.SHA1(text);
        assertEquals(expResult, result);
    }
}
