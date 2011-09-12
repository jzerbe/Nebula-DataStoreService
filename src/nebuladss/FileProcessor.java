/*
 * this class is used for encoding and decoding files from the TomP2P network
 * overlay. file pieces are stored on the network as byte arrays
 */
package nebuladss;

import contrib.TomP2P;
import java.util.ArrayList;
import java.util.Arrays;

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

    protected ArrayList<String> getFilePiecesList(String theFileNameStr) {
        byte[] aFileByteArray = TomP2P.getInstance().get(theFileNameStr);
        String aFileStr = aFileByteArray.toString();
        String[] aFileStrArray = aFileStr.split(kFilePiecesNameSplitStr);
        ArrayList<String> aArrayList = new ArrayList(Arrays.asList(aFileStrArray));
        return aArrayList;
    }

    protected void putFilePiecesList(String theFileNameStr, ArrayList<String> theFilePiecesList) {
        String aFileStr = "";
        for (int i = 0; i < theFilePiecesList.size(); i++) {
            aFileStr += theFilePiecesList.get(i) + kFilePiecesNameSplitStr;
        }
        TomP2P.getInstance().put(theFileNameStr, aFileStr.getBytes());
    }
}
