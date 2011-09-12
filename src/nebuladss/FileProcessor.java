/*
 * this class is used for encoding and decoding files from the TomP2P network
 * overlay. file pieces are stored on the network as byte arrays
 */
package nebuladss;

/**
 * @author Jason Zerbe
 */
public class FileProcessor implements ProgramConstants {

    protected String fp_BaseFileNameStr = null;

    public FileProcessor(String theBaseFileNameStr) {
        fp_BaseFileNameStr = theBaseFileNameStr;
    }

    public void combineFile() {
    }

    public void splitFile(int theChunkByteSize) {
    }

    public void splitFile() {
        splitFile(kDefaultFileChunkByteSize);
    }
}
