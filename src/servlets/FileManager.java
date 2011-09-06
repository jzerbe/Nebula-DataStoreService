/*
 * servlet that accepts POST requests containing a file to be saved and
 * other commands, also accepts GET request to retrieve file
 */
package servlets;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Jason Zerbe
 */
public class FileManager extends HttpServlet {

    public FileManager() {
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
    }
}
