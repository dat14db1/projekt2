import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.*;
import java.util.ArrayList;

public class SQLTest{
    //Variables to communicate with the db.
    static Connection conn = null;
    static Statement statement = null;
    static ResultSet resultSet = null;
    static String dbName = LoadDriver.dbName;
    private static PreparedStatement preparedStatement = null;


    //Variables to handle writing to the audit log text file
    static BufferedWriter writer = null;

    //Constructor that does setup
    public SQLTest() {
        //Set up log file.
        try {
            SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date timeNow = new Date();
            String stringDate = sdfDate.format(timeNow);
            writer = new BufferedWriter(new FileWriter("hospital_log.txt", true));
            writer.write("\n----Server started at " + stringDate + "----\n");
            writer.close();
        } catch (IOException ex) {
            System.out.println("Error when writing to file.");
        }

        //Connect to db.
        LoadDriver ld = new LoadDriver();
        try {
            conn = ld.conn;
            if (conn == null) System.out.println("Conn är null" );
            statement = conn.createStatement();

        } catch (SQLException ex) {
            // handle any errors
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
    }

    //Todo: list all available records, list all staff
    public static ArrayList<Record> listRecords(int personID){
        ArrayList<Record> list = new ArrayList<Record>();
        int roleID = 0; //IDs can never be zero in db.
        int personsDivisionID = 0;
        logAccessAttempt(7, 0, personID);
        try {
            //Check if person exists in db.
            preparedStatement = conn.prepareStatement("SELECT COUNT(*) FROM " + dbName + ".persons WHERE id = ?");
            preparedStatement.setInt(1, personID);
            resultSet = preparedStatement.executeQuery();
            //resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + dbName + ".persons WHERE id = " + personID);
            resultSet.next();
            if (resultSet.getInt(1) != 0) {
                //Get persons role and division
                preparedStatement = conn.prepareStatement("SELECT role_id, division_id FROM " + dbName + ".persons WHERE id = ?");
                preparedStatement.setInt(1, personID);
                resultSet = preparedStatement.executeQuery();
                //resultSet = statement.executeQuery("SELECT role_id, division_id FROM " + dbName + ".persons WHERE id = " + personID);
                resultSet.next();
                roleID = resultSet.getInt("role_id");
                personsDivisionID = resultSet.getInt("division_id");
            }
            switch (roleID) {
                case 1://Doctor - May read if the doctor id or division id matches.
                    preparedStatement = conn.prepareStatement("SELECT id FROM " + dbName + ".records WHERE doctor_id = ? or division_id = ?");
                    preparedStatement.setInt(1, personID);
                    preparedStatement.setInt(2, personsDivisionID);
                    resultSet = preparedStatement.executeQuery();
                    //resultSet = statement.executeQuery("SELECT id FROM " + dbName + ".records WHERE doctor_id = " + personID + " or division_id = " + personsDivisionID);
                    break;
                case 2://Nurse - May read if the nurse id or the division id matches.
                    preparedStatement = conn.prepareStatement("SELECT id FROM " + dbName + ".records WHERE nurse_id = ? or division_id = ?");
                    preparedStatement.setInt(1, personID);
                    preparedStatement.setInt(2, personsDivisionID);
                    resultSet = preparedStatement.executeQuery();
                    //resultSet = statement.executeQuery("SELECT id FROM " + dbName + ".records WHERE nurse_id = " + personID + " or division_id = " + personsDivisionID);
                    break;
                case 3://Patient - May read if the patient id matches.
                    preparedStatement = conn.prepareStatement("SELECT id FROM " + dbName + ".records WHERE patient_id = ?");
                    preparedStatement.setInt(1, personID);
                    resultSet = preparedStatement.executeQuery();
                    //resultSet = statement.executeQuery("SELECT id FROM " + dbName + ".records WHERE patient_id = " + personID);
                    break;
                case 4://Government agency - May read all records.
                    preparedStatement = conn.prepareStatement("SELECT id FROM " + dbName + ".records");
                    resultSet = preparedStatement.executeQuery();
                    break;
            }
            //Iterate through resultset and place in arraylist
            int temp = 0;
            ArrayList<Integer> ids = new ArrayList<Integer>();
            while (resultSet.next()) {
                temp = resultSet.getInt("id");
                ids.add(temp);
            }

            for (int id : ids) {
                list.add(new Record(id, conn, resultSet));
            }
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        return list;
    }


    /*Returns a Record object if the person has the right to access the record, otherwise it returns null*/
    public static Record readRecord(int recordID, int personID) {
        Record returnRecord = null;
        logAccessAttempt(1, recordID, personID);//1 indicates read attempt
        try {
            //Use COUNT(*) to check if the record exists
            preparedStatement = conn.prepareStatement("SELECT COUNT(*) FROM " + dbName + ".records WHERE id = ?");
            preparedStatement.setInt(1, recordID);
            resultSet = preparedStatement.executeQuery();
            //resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + dbName + ".records WHERE id = " + recordID);
            resultSet.next();
            if (resultSet.getInt(1) != 0) {
                System.out.println("Record found!");
                //Fetch record from db and place it in an object
                Record requestedRecord = new Record(recordID, conn, resultSet);

                //Check if the user has the right to read the record
                if (requestedRecord.checkReadPermission(personID, conn, resultSet)) {
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
    public static Record deleteRecord(int recordID, int personID) {
        Record returnRecord = null;
        logAccessAttempt(2, recordID, personID);//2 indicates delete attempt
        try {
            //Use COUNT(*) to check if the record exists
            preparedStatement = conn.prepareStatement("SELECT COUNT(*) FROM " + dbName + ".records WHERE id = ?");
            preparedStatement.setInt(1, recordID);
            resultSet = preparedStatement.executeQuery();
            //resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + dbName + ".records WHERE id = " + recordID);
            resultSet.next();
            if (resultSet.getInt(1) != 0) {
                System.out.println("Record found!");
                //Fetch record from db and place it in an object
                Record requestedRecord = new Record(recordID, conn, resultSet);

                //Check if the user has the right to delete the record
                if (requestedRecord.checkDeletePermission(personID, conn, resultSet)) {
                    returnRecord = requestedRecord;
                    requestedRecord.delete(recordID, conn, resultSet);
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
    public static Record updateRecord(int recordID, int personID, String newText) {
        Record returnRecord = null;
        logAccessAttempt(3, recordID, personID);//3 indicates update attempt
        System.out.println("här då ?");
        try {
            //Use COUNT(*) to check if the record exists
            System.out.println("här");
            preparedStatement = conn.prepareStatement("SELECT COUNT(*) FROM " + dbName + ".records WHERE id = ?");
            preparedStatement.setInt(1, recordID);
            resultSet = preparedStatement.executeQuery();
            //resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + dbName + ".records WHERE id = " + recordID);
            resultSet.next();
            if (resultSet.getInt(1) != 0) {
                System.out.println("Record found!");
                //Fetch record from db and place it in an object
                Record requestedRecord = new Record(recordID, conn, resultSet);

                //Check if the user has the right to update the record
                if (requestedRecord.checkUpdatePermission(personID, conn, resultSet)) {
                    requestedRecord.update(recordID, newText, conn, resultSet);
                    //Value to return must be the updated one.
                    returnRecord = new Record(recordID, conn, resultSet);
                    System.out.println("Update Access granted.");
                } else {
                    System.out.println("Update Access denied.");
                }
            } else {
                System.out.println("getint är noll.");
            }
        } catch (SQLException ex) {
            System.out.println("SQLException: i denna" + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        return returnRecord;
    }

    /*Creates a new record. Returns the new record if the creation succeeds, otherwise null.*/
    public static Record createRecord(int personID, int nurseID, int patientID, int divisionID, String text) {
        Record returnRecord = null;
        logAccessAttempt(4, 0, personID);//4 indicates update attempt
        //Check if person is a doctor. The method is static so no object is needed.
        if (Record.checkCreatePermission(personID, conn, resultSet)) {
            //Create here.
            int newID = Record.create(personID, nurseID, patientID, divisionID, text, conn, resultSet);
            logAccessAttempt(5, 0, personID);//5 indicates successful update
            returnRecord = readRecord(personID, newID);
            System.out.println("Create access granted.");
        } else {
            System.out.println("Create access denied.");
        }
        return returnRecord;
    }

    public int createPerson(int creatorID, String newpersonName, int newpersonRole, int newpersonDivision){
        int id = -1;
        logAccessAttempt(6, 0, creatorID);
        if (Record.checkCreatePermission(creatorID, conn, resultSet)) {
            id = Record.newPerson(newpersonName, newpersonRole, newpersonDivision, conn, resultSet);
        }
        return id;
    }

    public static boolean checkPersonRole(int personID, int reqRole){
        boolean bool = Record.checkPersonRole(personID, conn, resultSet) == reqRole ? true : false ;
        return bool;
    }

    public static boolean checkDivExist(int division) {
        return Record.checkDivisionExists(division, conn, resultSet);
    }

    /*Writes all access attempts to a text file.*/
    private static void logAccessAttempt(int operation, int recordID, int personID) {
        //Write an access attempt to the log file.
        String operationName = "";
        switch (operation) {
            case 1://Read attempt
                operationName = "Read record";
                break;
            case 2://Delete attempt
                operationName = "Delete record";
                break;
            case 3://Update attempt
                operationName = "Update record";
                break;
            case 4:
                operationName = "Create record attempt";
                break;
            case 5://New record successfullt created
                operationName = "Create record successful";
                break;
            case 6:
                operationName = "Create patient";
                break;
            case 7:
                operationName = "List records";
                break;
            default:
                operationName = "Unknown operation";
                break;
        }
        //Get the time for the log file.
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date timeNow = new Date();
        String stringDate = sdfDate.format(timeNow);
        try {
            writer = new BufferedWriter(new FileWriter("hospital_log.txt", true));//True makes the program append
            if (operation == 5) {//Create
                writer.write(operationName + " attempt by user with id " + personID + " at " + stringDate + ".\n");
            } else if(operation == 7){
                writer.write(operationName + " by user with id " + personID + " at " + stringDate + ".\n");
            } else if(operation == 6){
                writer.write(operationName + " attempt by user with id " + personID + " at " + stringDate + ".\n");
            } else {
                writer.write(operationName + " attempt of record with id " + recordID +
                    " by user with id " + personID + " at " + stringDate + ".\n");
            }
            writer.close();
        } catch (IOException ex) {
            System.out.println("Error when writing to file.");
        }
    }
}
