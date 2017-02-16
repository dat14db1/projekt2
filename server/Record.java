import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.Date;
import java.io.*;

public class Record {
	private static String dbName = LoadDriver.dbName;
	public int id;
	public int patientID;
	public int doctorID;
	public int nurseID;
	public int divisionID;
	public String text;
	public String patientName;
	public String doctorName;
	public String nurseName;
	public String divisionName;

	public Record(int id, Statement statement, ResultSet resultSet) {
		this.id = id;
		try {
			//Fetch data from db
			resultSet = statement.executeQuery("SELECT * FROM " + dbName + ".records WHERE records.id = " + id);
			resultSet.next();
			//Place data in object fields
			patientID = resultSet.getInt("patient_id");
			doctorID = resultSet.getInt("doctor_id");
			nurseID = resultSet.getInt("nurse_id");
			divisionID = resultSet.getInt("division_id");
			text = resultSet.getString("text");

			//Fetch doctor name
			resultSet = statement.executeQuery("SELECT name FROM " + dbName + ".persons WHERE id = " + doctorID);
			resultSet.next();
			doctorName = resultSet.getString(1);

			//Fetch patient name
			resultSet = statement.executeQuery("SELECT name FROM " + dbName + ".persons WHERE id = " + patientID);
			resultSet.next();
			patientName = resultSet.getString(1);

			//Fetch nurse name
			resultSet = statement.executeQuery("SELECT name FROM " + dbName + ".persons WHERE id = " + nurseID);
			resultSet.next();
			nurseName = resultSet.getString(1);

			//Fetch division name
			resultSet = statement.executeQuery("SELECT name FROM " + dbName + ".divisions WHERE id = " + divisionID);
			resultSet.next();
			divisionName = resultSet.getString(1);
		} catch(SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
		}
	}
	public boolean checkReadPermission(int personID, Statement statement, ResultSet resultSet) {
		boolean readOk = false;
		int roleID = 0; //IDs can never be zero in db.
		int personsDivisionID = 0;
		try {
			//Check if person exists in db.
			resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + dbName + ".persons WHERE id = " + personID);
			resultSet.next();
			if (resultSet.getInt(1) != 0) {
				//Get persons role and division
				resultSet = statement.executeQuery("SELECT role_id, division_id FROM " + dbName + ".persons WHERE id = " + personID);
				resultSet.next();
				roleID = resultSet.getInt("role_id");
				personsDivisionID = resultSet.getInt("division_id");
			}
		} catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
		}

		//Decide what to do depending on the role of the requestor.
		switch (roleID) {
			case 1://Doctor - May read if the doctor id or division id matches.
				if (doctorID == personID || divisionID == personsDivisionID) {
					readOk = true;
				}
				break;
			case 2://Nurse - May read if the nurse id or the division id matches.
				if (nurseID == personID || divisionID == personsDivisionID) {
					readOk = true;
				}
				break;
			case 3://Patient - May read if the patient id matches.
				if (patientID == personID) {
					readOk = true;
				}
				break;
			case 4://Government agency - May read all records.
				readOk = true;
				break;
		}
		return readOk;
	}

	// returns roleID of person with personID, or -1 if person does not exist 
    public static int checkPersonRole(int personID, Statement statement, ResultSet resultSet){
        try { 
        	resultSet = statement.executeQuery("SELECT role_id FROM " + dbName + ".persons WHERE id = " + personID);
        	resultSet.next();
        	return resultSet.getInt(1);
        } catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
		}
		return -1;
    }

    public static boolean checkDivisionExists(int divisionID, Statement statement, ResultSet resultSet) {
    	boolean exists = false; 
    	try {
    		resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + dbName + ".divisions WHERE id = " + divisionID);
    		resultSet.next();
    		exists = (resultSet.getInt(1) == 0) ? false : true;
    	}

    	catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
		}
		return exists;
    }

	public boolean checkDeletePermission(int personID, Statement statement, ResultSet resultSet) {
		boolean deleteOk = false;
		int roleID = 0; //IDs can never be zero in db.
		try {
			//Check if person exists in db.
			resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + dbName + ".persons WHERE id = " + personID);
			resultSet.next();
			if (resultSet.getInt(1) != 0) {
				//Get persons role
				resultSet = statement.executeQuery("SELECT role_id FROM " + dbName + ".persons WHERE id = " + personID);
				resultSet.next();
				roleID = resultSet.getInt("role_id");
			}
		} catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
		}

		//Only government agencies are allowed to delete a record.
		if (roleID == 4) {//4 is agency
			deleteOk = true;
		}
		return deleteOk;
	}

	public boolean checkUpdatePermission(int personID, Statement statement, ResultSet resultSet) {
		boolean updateOk = false;
		int roleID = 0; //IDs can never be zero in db.
		try {
			//Check if person exists in db.
			resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + dbName + ".persons WHERE id = " + personID);
			resultSet.next();
			if (resultSet.getInt(1) != 0) {
				//Get persons role
				resultSet = statement.executeQuery("SELECT role_id FROM " + dbName + ".persons WHERE id = " + personID);
				resultSet.next();
				roleID = resultSet.getInt("role_id");
			}
		} catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
		}

		//Decide what to do depending on the role of the requestor.
		switch (roleID) {
			case 1://Doctor - May update if the doctor id matches.
				if (doctorID == personID) {
					updateOk = true;
				}
				break;
			case 2://Nurse - May read if the nurse id matches.
				if (nurseID == personID) {
					updateOk = true;
				}
				break;
		}
		return updateOk;
	}

	/*Checks if a person is a doctor, since only doctors have create permission.*/
    public static boolean checkCreatePermission(int personID, Statement statement, ResultSet resultSet) {
        int roleID = 0; //IDs can never be zero in db.
        try {
            //Check if person exists in db.
            resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + dbName + ".persons WHERE id = " + personID);
            resultSet.next();
            if (resultSet.getInt(1) != 0) {
                //Get persons role
                resultSet = statement.executeQuery("SELECT role_id FROM " + dbName + ".persons WHERE id = " + personID);
                resultSet.next();
                roleID = resultSet.getInt("role_id");
            }
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        return roleID == 1;//1 indicates doctor
    }

    /*Creates new record and returns the id of the new record. Returns 0 if the creation fails.*/
    public static int create(int personID, int nurseID, int patientID, int divisionID, String text, 
    		Statement statement, ResultSet resultSet) {
    	int newID = 0;
        try {
            //executeUpdate() is used when the table is altered by the statement.
            statement.executeUpdate("INSERT INTO " + dbName + ".records VALUES(default, '" + 
                text + "', " + patientID + ", " + personID + ", " + nurseID + ", " +
                divisionID + ");");
            //Get the ID of the new record.
            resultSet = statement.executeQuery("SELECT id FROM " + dbName + ".records ORDER BY id desc LIMIT 1");
            resultSet.next();
            newID = resultSet.getInt(1);
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        return newID;
    }

	public void update(int recordID, String newText, Statement statement, ResultSet resultSet) {
		try {
			//executeUpdate() is used when the table is altered by the statement.
			statement.executeUpdate("UPDATE " + dbName + ".records SET text = '" + newText
				+ "' WHERE id = " + recordID);
		} catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
		}
	}

	public void delete(int recordID, Statement statement, ResultSet resultSet) {
		try {
			//executeUpdate() is used when the table is altered by the statement.
			statement.executeUpdate("DELETE FROM " + dbName + ".records WHERE id = " + recordID);
		} catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
		}
	}
	public static int newPerson(String personName, int roleID, int divisionID, Statement statement, ResultSet resultSet){
		int id = -1;
		try {
			statement.executeUpdate("INSERT INTO " + dbName + ".persons VALUES(default, '" + personName + "', " + roleID + ", " + divisionID + ");");
			//Check id of new patient.
			resultSet = statement.executeQuery("SELECT id FROM " + dbName + ".persons ORDER BY id desc LIMIT 1");
			resultSet.next();
			id = resultSet.getInt(1);
		} catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
		}
		return id;
	}
}