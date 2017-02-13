import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.io.*;

//This class sets up a connection to a local MySQL database.

public class LoadDriver {
	public Statement statement;
	public ResultSet resultSet;
	public Connection conn;
    public LoadDriver() {
        try {
            // The newInstance() call is a work around for some
            // broken Java implementations

            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (Exception ex) {
            // handle the error
        	System.out.println("Could not find mysql libraries.");
        }

   		conn = null;
		try {
		    conn =
		       DriverManager.getConnection("jdbc:mysql://localhost/hospital_db?" +
		                                   "user=hospital&password=password");

		    // Do something with the Connection

		} catch (SQLException ex) {
		    // handle any errors
		    System.out.println("SQLException: " + ex.getMessage());
		    System.out.println("SQLState: " + ex.getSQLState());
		    System.out.println("VendorError: " + ex.getErrorCode());
		}
    }
}