/*
 * main class for application
 */
package nebuladss;

/**
 * @author Jason Zerbe
 */
public class NebulaDSS implements ProgramConstants {

    /**
     * master start of application
     * 
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        for (String currentArg : args) {
            if (currentArg.contains("-h")) {
                System.out.println(kUsageStr);
                System.exit(0);
            }
        } //done processing arguments
        
        //get bootstrap nodes from webcache via nodemanger (singleton)
        
        //ping bootstrap nodes
        
        //get "close by" nodes from bootstrap nodes (first time), get closer nodes from previously close nodes
    }
}
