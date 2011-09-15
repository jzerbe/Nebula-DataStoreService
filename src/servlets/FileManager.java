/*
 * servlet that accepts POST requests containing a file to be saved and
 * other commands, also accepts GET request to retrieve file
 */
package servlets;

import contrib.TomP2P.TomP2P;
import java.io.File;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nebuladss.ProgramConstants;
import nebuladss.ProgramConstants.enumOperationType;

/**
 * handles POST requests for PUT, GET, DELETE file operations
 * handles GET requests for GET file operation
 * 
 * @author Jason Zerbe
 */
public class FileManager extends HttpServlet implements ProgramConstants {

    public FileManager() {
    }

    /**
     * check if the name of the file already exists in the network
     * @param theFileNameStr String
     * @return boolean - does the file exist?
     */
    protected boolean fileExistsInNetwork(String theFileNameStr) {
        if (TomP2P.getInstance().get(theFileNameStr) == null) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        String aOperationString = req.getParameter("operation");
        if (aOperationString.equals(enumOperationType.PUT.toString())) {
            File aFileUpload = (File) req.getAttribute("file");
            if ((aFileUpload == null) || fileExistsInNetwork(aFileUpload.getName())) {
                resp.setStatus(403);
            } else {
                //break file into pieces and push into TomP2P
            }
        } else if (aOperationString.equals(enumOperationType.DELETE.toString())) {
            String aFileStr = req.getParameter("file");
            TomP2P.getInstance().delete(aFileStr);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        String aOperationString = req.getParameter("operation");
        if (aOperationString.equals(enumOperationType.GET.toString())) {
            String aFileStr = req.getParameter("file");
            if (!fileExistsInNetwork(aFileStr)) {
                resp.setStatus(404);
            } else {
                //get file pieces from TomP2P MMT
            }
        }
    }
}
