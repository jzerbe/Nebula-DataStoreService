/*
 * servlet that accepts POST requests containing a file to be saved and
 * other commands, also accepts GET request to retrieve file
 */
package servlets;

import java.io.File;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nebuladss.ProgramConstants.enumOperationType;

/**
 * @author Jason Zerbe
 */
public class FileManager extends HttpServlet {

    public FileManager() {
    }

    /**
     * check if the file to be uploaded already exists in the network
     * @param theFile File
     * @return boolean - does the file exist?
     */
    boolean fileExists(File theFile) {
        if (theFile.exists()) {
            return true;
        }
        //TODO: check the Kademlia network for the file
        return false;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        String aOperationString = req.getParameter("operation");
        if (aOperationString.equals(enumOperationType.GET.toString())) {
            String aFileStr = req.getParameter("file");
            //get file pieces from TomP2P MMT
        } else if (aOperationString.equals(enumOperationType.PUT.toString())) {
            File aFileUpload = (File) req.getAttribute("file");
            //break file into pieces and push into TomP2P
        } else if (aOperationString.equals(enumOperationType.DELETE.toString())) {
            String aFileStr = req.getParameter("file");
            //replace file with expire flags on all MMT nodes
        }
    }
}
