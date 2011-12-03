/*
 * servlet that:
 * accepts POST requests containing a file to be saved
 * accepts GET request to retrieve file
 */
package servlets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nebuladss.FileSystemManager;
import nebuladss.HttpCmdClient;
import nebuladss.ProgramConstants;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 * handles POST requests for PUT file operation
 * handles GET requests for GET file operation
 *
 * @author Jason Zerbe
 */
public class FileManager extends HttpServlet implements ProgramConstants {

    private static final long serialVersionUID = 42L;
    private File myTmpDir;

    public FileManager() {
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        myTmpDir = new File(FileSystemManager.getInstance().getStorageTmpPath());
        if (!myTmpDir.exists()) {
            createDirectory(FileSystemManager.getInstance().getStorageTmpPath());
        }
        if (!myTmpDir.isDirectory()) {
            throw new ServletException(FileSystemManager.getInstance().getStorageTmpPath() + " is not a directory");
        }

        String rootPath = FileSystemManager.getInstance().getStorageRootPath();
        File aDestDir = new File(rootPath);
        if (!aDestDir.exists()) {
            createDirectory(rootPath);
        }
        if (!aDestDir.isDirectory()) {
            throw new ServletException(FileSystemManager.getInstance().getStorageRootPath() + " is not a directory");
        }

    }

    /**
     * based largely on http://www.servletworld.com/servlet-tutorials/servlet-file-upload-example.html
     * @param req HttpServletRequest
     * @param resp HttpServletResponse
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        resp.addHeader("Access-Control-Allow-Origin", "*"); //http://enable-cors.org/#how-cgi

        if (ServletFileUpload.isMultipartContent(req)) {

            String namespace = "";
            String filename = "";

            DiskFileItemFactory aDiskFileItemFactory = new DiskFileItemFactory();
            aDiskFileItemFactory.setSizeThreshold(1 * 1024 * 1024); //copy to disk if >1MB
            aDiskFileItemFactory.setRepository(myTmpDir); //storing files above threshold
            ServletFileUpload aServletFileUpload = new ServletFileUpload(aDiskFileItemFactory);

            try {
                List<FileItem> items = (List<FileItem>) aServletFileUpload.parseRequest(req);
                for (FileItem item : items) {
                    if (item.isFormField()) {
                        if (item.getFieldName().equals("namespace")) {
                            namespace = item.getString();
                        }
                        if (item.getFieldName().equals("filename")) {
                            filename = item.getString();
                        }
                    } else {
                        String aNewFilePathStr = FileSystemManager.getInstance().getFormattedFilePathStr(
                                FileSystemManager.getInstance().getStorageRootPath(),
                                namespace, filename);
                        File aFile = new File(aNewFilePathStr);
                        if (aFile.exists()) {
                            Logger.getLogger(FileManager.class.getName()).log(
                                    Level.INFO, "file already exists: {0}", aNewFilePathStr);
                            return; //file already in system - do nothing
                        }

                        if (aFile.exists()) {
                            return; //file already in system - do nothing
                        }

                        item.write(aFile);
                    }
                }
            } catch (FileUploadException ex) {
                Logger.getLogger(FileManager.class.getName()).log(
                        Level.SEVERE, "Error encountered while parsing the request", ex);
                return;
            } catch (Exception ex) {
                Logger.getLogger(FileManager.class.getName()).log(
                        Level.SEVERE, "Error encountered while uploading file", ex);
                return;
            }

            HttpCmdClient.getInstance().putFile(namespace, filename); //log to master server

            //output success response
            try {
                resp.getWriter().println("Saved: " + namespace + "-" + filename);
            } catch (IOException ex) {
                Logger.getLogger(FileManager.class.getName()).log(Level.WARNING, null, ex);
            }
        } else {
            Logger.getLogger(FileManager.class.getName()).log(
                    Level.WARNING, "Not valid multipart POST {0}-{1}",
                    new Object[]{req.getParameter("namespace"),
                        req.getParameter("filename")});
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        resp.addHeader("Access-Control-Allow-Origin", "*"); //http://enable-cors.org/#how-cgi

        String namespace = req.getParameter("namespace");
        String filename = req.getParameter("filename");

        if (((namespace != null) && (!namespace.equals("")))
                && ((filename != null) && (!filename.equals("")))) {

            //get the file object requested
            File aFile = new File(FileSystemManager.getInstance().getFormattedFilePathStr(
                    FileSystemManager.getInstance().getStorageRootPath(),
                    namespace, filename));

            //send the file contents to the requester (if file exists)
            if (aFile != null) {
                try {
                    doDownload(req, resp, aFile.getCanonicalPath(), filename);
                } catch (IOException ex) {
                    Logger.getLogger(FileManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else {
            try {
                resp.getWriter().println(
                        "Local FS Available: "
                        + String.valueOf(FileSystemManager.getInstance().getCurrentAvailableMegaBytes())
                        + "MB / "
                        + String.valueOf(FileSystemManager.getInstance().getMaxAvailableMegaBytes())
                        + "MB");
            } catch (IOException ex) {
                Logger.getLogger(FileManager.class.getName()).log(Level.WARNING, null, ex);
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

    protected void createDirectory(String theDirPathStr) {
        File aNewDirectory = new File(theDirPathStr);
        if (!aNewDirectory.mkdir()) {
            if (!aNewDirectory.mkdirs()) {
                System.err.println("failed to create '" + theDirPathStr + "' directory");
            }
        }
    }
}
