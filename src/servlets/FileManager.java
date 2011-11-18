/*
 * servlet that:
 * accepts POST requests containing a file to be saved
 * accepts GET request to retrieve file
 */
package servlets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nebuladss.FileSystemManager;
import nebuladss.ProgramConstants;

/**
 * handles POST requests for PUT file operation
 * handles GET requests for GET file operation
 * 
 * @author Jason Zerbe
 */
public class FileManager extends HttpServlet implements ProgramConstants {

    public FileManager() {
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        File aFileUpload = (File) req.getAttribute("file");
        if (aFileUpload != null && aFileUpload.exists() && !aFileUpload.isDirectory()) {
            String filename = req.getParameter("filename");
            String namespace = req.getParameter("namespace");

            if (((filename != null) && (!filename.equals("")))
                    && ((namespace != null) && (!namespace.equals("")))) {

                //push the file object and associated parameters to the filesystem (or other node)
                FileSystemManager.getInstance().putFile(namespace, filename, aFileUpload);
            } else {
                System.err.println("problem with namespace or filename parameters");
            }
        } else if (aFileUpload == null) {
            System.err.println("aFileUpload is null");
        } else {
            System.err.println("problem with aFileUpload");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        String opt = req.getParameter("opt"); //what are we doing?
        if ((opt != null) && (!opt.equals(""))) { //non-empty
            if (opt.equals("get")) {
                String filename = req.getParameter("filename");
                String namespace = req.getParameter("namespace");

                if (((filename != null) && (!filename.equals("")))
                        && ((namespace != null) && (!namespace.equals("")))) {

                    //get the file object requested
                    File aFile = FileSystemManager.getInstance().getFile(namespace, filename);

                    //send the file contents to the requester (if file exists)
                    if (aFile != null) {
                        try {
                            doDownload(req, resp, aFile.getCanonicalPath(), aFile.getName());
                        } catch (IOException ex) {
                            Logger.getLogger(FileManager.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        }
    }

    /**
     * combination of source code from
     * [http://stackoverflow.com/questions/685271/using-servletoutputstream-to-write-very-large-files-in-a-java-servlet-without-mem]
     * and [http://snippets.dzone.com/posts/show/4629] that allows for the
     * pass-through downloading of data over HTTP 1.1 from the datastore node
     * 
     * @param req HttpServletRequest
     * @param resp HttpServletResponse
     * @param theOriginalFileName String
     * @param theOutputFileName String
     * @throws IOException 
     */
    protected void doDownload(HttpServletRequest req, HttpServletResponse resp,
            String theOriginalFileName, String theOutputFileName) throws IOException {
        FileInputStream aFileInputStream = null;

        try {
            aFileInputStream = new FileInputStream(theOriginalFileName);
            String aMimeType = getServletConfig().getServletContext().getMimeType(theOriginalFileName);
            resp.setContentType((aMimeType != null) ? aMimeType : "application/octet-stream");
            resp.setContentLength((int) (new File(theOriginalFileName)).length());
            resp.setHeader("Content-Disposition", "attachment; filename=\"" + theOutputFileName + "\"");

            byte[] buffer = new byte[1024];
            int bytesRead = 0;

            do {
                bytesRead = aFileInputStream.read(buffer, 0, buffer.length);
                resp.getOutputStream().write(buffer, 0, bytesRead);
            } while (bytesRead == buffer.length);

            resp.getOutputStream().flush();
        } finally {
            if (aFileInputStream != null) {
                aFileInputStream.close();
            }
        }
    }
}
