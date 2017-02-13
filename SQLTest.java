import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.*;

public class SQLTest{
    //Variables to communicate with the db.
    static Connection conn = null;
    static Statement statement = null;
    static ResultSet resultSet = null;

    //Variables to handle writing to the audit log text file
    static BufferedWriter writer = null;

	public static void main(String args[]) {
        //Set up log file.
        try {
            writer = new BufferedWriter(new FileWriter("hospital_log.txt"));
            writer.write("Beginning of log file.\n");
            writer.close();
        } catch (IOException ex) {
            System.out.println("Error when writing to file.");
        }

        //Connect to db.
		LoadDriver ld = new LoadDriver();
        try {
    		conn = ld.conn;
    		statement = conn.createStatement();
            readRecord(2, 5);
            //deleteRecord(2, 4);
            //updateRecord(3, 1, "Huvudvärk");
            createRecord(2, 2, 3, 2, "Ont i höger stortå.");

        } catch (SQLException ex) {
            // handle any errors
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
	}

    /*Returns a Record object if the person has the right to access the record, otherwise it returns null*/
    private static Record readRecord(int recordID, int personID) {
        Record returnRecord = null;
        logAccessAttempt(1, recordID, personID);//1 indicates read attempt
        try {
            //Use COUNT(*) to check if the record exists
            resultSet = statement.executeQuery("SELECT COUNT(*) FROM hospital_db.records WHERE id = " + recordID);
            resultSet.next();
            if (resultSet.getInt(1) != 0) {
                System.out.println("Record found!");
                //Fetch record from db and place it in an object
                Record requestedRecord = new Record(recordID, statement, resultSet);

                //Check if the user has the right to read the record
                if (requestedRecord.checkReadPermission(personID, statement, resultSet)) {
                    returnRecord = requestedRecord;
                    System.out.println("Read Access granted.");
                } else {
                    System.out.println("Read Access denied.");
                }
            }
            System.out.println("Record not found.");
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        return returnRecord;
    }

    /*Returns the deleted Record object if the deletion succeeds, otherwise null.*/
    private static Record deleteRecord(int recordID, int personID) {
        Record returnRecord = null;
        logAccessAttempt(2, recordID, personID);//2 indicates delete attempt
        try {
        //Use COUNT(*) to check if the record exists
            resultSet = statement.executeQuery("SELECT COUNT(*) FROM hospital_db.records WHERE id = " + recordID);
            resultSet.next();
            if (resultSet.getInt(1) != 0) {
                System.out.println("Record found!");
                //Fetch record from db and place it in an object
                Record requestedRecord = new Record(recordID, statement, resultSet);

                //Check if the user has the right to delete the record
                if (requestedRecord.checkDeletePermission(personID, statement, resultSet)) {
                    returnRecord = requestedRecord;
                    requestedRecord.delete(recordID, statement, resultSet);
                    System.out.println("Delete Access granted.");
                } else {
                    System.out.println("Delete Access denied.");
                }
            }
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        return returnRecord;
    }

    /*Updates the text of the selected record. Returns the updated version of the record if it succeeds, otherwise null*/
    private static Record updateRecord(int recordID, int personID, String newText) {
        Record returnRecord = null;
        logAccessAttempt(3, recordID, personID);//3 indicates update attempt
        try {
        //Use COUNT(*) to check if the record exists
            resultSet = statement.executeQuery("SELECT COUNT(*) FROM hospital_db.records WHERE id = " + recordID);
            resultSet.next();
            if (resultSet.getInt(1) != 0) {
                System.out.println("Record found!");
                //Fetch record from db and place it in an object
                Record requestedRecord = new Record(recordID, statement, resultSet);

                //Check if the user has the right to update the record
                if (requestedRecord.checkUpdatePermission(personID, statement, resultSet)) {
                    requestedRecord.update(recordID, newText, statement, resultSet);
                    //Value to return must be the updated one.
                    returnRecord = new Record(recordID, statement, resultSet);
                    System.out.println("Update Access granted.");
                } else {
                    System.out.println("Update Access denied.");
                }
            }
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        return returnRecord;
    }

    private static Record createRecord(int personID, int nurseID, int patientID, int divisionID, String text) {
        Record returnRecord = null;
        logAccessAttempt(4, 0, personID);//4 indicates update attempt
        //Check if person is a doctor. The method is static so no object is needed.
        if (Record.checkCreatePermission(personID, statement, resultSet)) {
            //Create here.
            int newID = Record.create(personID, nurseID, patientID, divisionID, text, statement, resultSet);
            logAccessAttempt(5, 0, personID);//5 indicates successful update
            returnRecord = readRecord(personID, newID);
            System.out.println("Create access granted.");
        } else {
            System.out.println("Create access denied.");
        }
        return returnRecord;
    }


    private static void logAccessAttempt(int operation, int recordID, int personID) {
        //Write an access attempt to the log file.
        String operationName = "";
        switch (operation) {
            case 1://Read attempt
                operationName = "Read";
                break;
            case 2://Delete attempt
                operationName = "Delete";
                break;
            case 3://Update attempt
                operationName = "Update";
                break;
            case 5://New record successfullt created
                operationName = "Create";
                break; 
            default:
                operationName = "Unknown operation";
        }
        //Get the time for the log file.
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date timeNow = new Date();
        String stringDate = sdfDate.format(timeNow);
        try {
            writer = new BufferedWriter(new FileWriter("hospital_log.txt", true));//True makes the program append
            if (operation != 4) {//Read, write, update
                writer.write(operationName + " attempt of record with id " + recordID +
                    " by user with id " + personID + " at " + stringDate + ".\n");
            } else {//Create
                writer.write("Create attempt by user with id " + personID + " at " + stringDate + ".\n");
            }
            writer.close();
        } catch (IOException ex) {
            System.out.println("Error when writing to file.");
        }
    }
}