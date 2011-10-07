/*
 * servlet that accepts POST requests containing a file to be saved and
 * other commands, also accepts GET request to retrieve file
 */
package servlets;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
    }
}
