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
	private PreparedStatement preparedStatement = null;

	public Record(int id, Connection conn, ResultSet resultSet) {
		this.id = id;
		try {
			//Fetch data from db
			preparedStatement = conn.prepareStatement("SELECT * FROM " + dbName + ".records WHERE records.id = ?");
			preparedStatement.setInt(1, id);
			resultSet = preparedStatement.executeQuery();
			//resultSet = statement.executeQuery("SELECT * FROM " + dbName + ".records WHERE records.id = " + id);
			resultSet.next();
			//Place data in object fields
			patientID = resultSet.getInt("patient_id");
			doctorID = resultSet.getInt("doctor_id");
			nurseID = resultSet.getInt("nurse_id");
			divisionID = resultSet.getInt("division_id");
			text = resultSet.getString("text");

			//Fetch doctor name
			preparedStatement = conn.prepareStatement("SELECT name FROM " + dbName + ".persons WHERE id = ?");
			preparedStatement.setInt(1, doctorID);
			resultSet = preparedStatement.executeQuery();
			//resultSet = statement.executeQuery("SELECT name FROM " + dbName + ".persons WHERE id = " + doctorID);
			resultSet.next();
			doctorName = resultSet.getString(1);

			//Fetch patient name
			preparedStatement = conn.prepareStatement("SELECT name FROM " + dbName + ".persons WHERE id = ?");
			preparedStatement.setInt(1, patientID);
			resultSet = preparedStatement.executeQuery();
			//resultSet = statement.executeQuery("SELECT name FROM " + dbName + ".persons WHERE id = " + patientID);
			resultSet.next();
			patientName = resultSet.getString(1);

			//Fetch nurse name
			preparedStatement = conn.prepareStatement("SELECT name FROM " + dbName + ".persons WHERE id = ?");
			preparedStatement.setInt(1, nurseID);
			resultSet = preparedStatement.executeQuery();
			//resultSet = statement.executeQuery("SELECT name FROM " + dbName + ".persons WHERE id = " + nurseID);
			resultSet.next();
			nurseName = resultSet.getString(1);

			//Fetch division name
			preparedStatement = conn.prepareStatement("SELECT name FROM " + dbName + ".divisions WHERE id = ?");
			preparedStatement.setInt(1, divisionID);
			resultSet = preparedStatement.executeQuery();
			//resultSet = statement.executeQuery("SELECT name FROM " + dbName + ".divisions WHERE id = " + divisionID);
			resultSet.next();
			divisionName = resultSet.getString(1);
		} catch(SQLException ex) {
            System.out.println("SQLException: i konstruktorn" + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
		}
	}
	public boolean checkReadPermission(int personID, Connection conn, ResultSet resultSet) {
		boolean readOk = false;
		int roleID = 0; //IDs can never be zero in db.
		int personsDivisionID = 0;
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
    public static int checkPersonRole(int personID, Connection conn, ResultSet resultSet){
        try {
        	PreparedStatement prepStat = conn.prepareStatement("SELECT role_id FROM " + dbName + ".persons WHERE id = ?");
        	prepStat.setInt(1, personID);
			resultSet = prepStat.executeQuery();
        	//resultSet = statement.executeQuery("SELECT role_id FROM " + dbName + ".persons WHERE id = " + personID);
        	resultSet.next();
        	return resultSet.getInt(1);
        } catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
		}
		return -1;
    }

    public static boolean checkDivisionExists(int divisionID, Connection conn, ResultSet resultSet) {
    	boolean exists = false;
    	try {
    		PreparedStatement prepStat = conn.prepareStatement("SELECT COUNT(*) FROM " + dbName + ".divisions WHERE id = ?	");
        	prepStat.setInt(1, divisionID);
			resultSet = prepStat.executeQuery();
    		//resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + dbName + ".divisions WHERE id = " + divisionID);
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

	public boolean checkDeletePermission(int personID, Connection conn, ResultSet resultSet) {
		boolean deleteOk = false;
		int roleID = 0; //IDs can never be zero in db.
		try {
			//Check if person exists in db.
			preparedStatement = conn.prepareStatement("SELECT COUNT(*) FROM " + dbName + ".persons WHERE id = ?");
			preparedStatement.setInt(1, personID);
			resultSet = preparedStatement.executeQuery();
			//resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + dbName + ".persons WHERE id = " + personID);
			resultSet.next();
			if (resultSet.getInt(1) != 0) {
				//Get persons role
				preparedStatement = conn.prepareStatement("SELECT role_id FROM " + dbName + ".persons WHERE id = ?");
				preparedStatement.setInt(1, personID);
				resultSet = preparedStatement.executeQuery();
				//resultSet = statement.executeQuery("SELECT role_id FROM " + dbName + ".persons WHERE id = " + personID);
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

	public boolean checkUpdatePermission(int personID, Connection conn, ResultSet resultSet) {
		System.out.println("i checkUpdatePermission");
		boolean updateOk = false;
		int roleID = 0; //IDs can never be zero in db.
		try {
			//Check if person exists in db.
			preparedStatement = conn.prepareStatement("SELECT COUNT(*) FROM " + dbName + ".persons WHERE id = ?");
			preparedStatement.setInt(1, personID);
			resultSet = preparedStatement.executeQuery();
			//resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + dbName + ".persons WHERE id = " + personID);
			resultSet.next();
			if (resultSet.getInt(1) != 0) {
				//Get persons role
				preparedStatement = conn.prepareStatement("SELECT role_id FROM " + dbName + ".persons WHERE id = ?");
				preparedStatement.setInt(1, personID);
				resultSet = preparedStatement.executeQuery();
				//resultSet = statement.executeQuery("SELECT role_id FROM " + dbName + ".persons WHERE id = " + personID);
				resultSet.next();
				roleID = resultSet.getInt("role_id");
				System.out.println("role id: " + roleID);
			} else {
				System.out.println("No person found.");
			}
		} catch (SQLException ex) {
			System.out.println("SQLException i checkUpdatePermission: " + ex.getMessage());
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
    public static boolean checkCreatePermission(int personID, Connection conn, ResultSet resultSet) {
        int roleID = 0; //IDs can never be zero in db.
        try {
            //Check if person exists in db.
            PreparedStatement prepStat = conn.prepareStatement("SELECT COUNT(*) FROM " + dbName + ".persons WHERE id = ?");
        	prepStat.setInt(1, personID);
			resultSet = prepStat.executeQuery();
            //resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + dbName + ".persons WHERE id = " + personID);
            resultSet.next();
            if (resultSet.getInt(1) != 0) {
                //Get persons role
                prepStat = conn.prepareStatement("SELECT role_id FROM " + dbName + ".persons WHERE id = ?");
        		prepStat.setInt(1, personID);
				resultSet = prepStat.executeQuery();
                //resultSet = statement.executeQuery("SELECT role_id FROM " + dbName + ".persons WHERE id = " + personID);
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
    		Connection conn, ResultSet resultSet) {
    	int newID = 0;
        try {
            //executeUpdate() is used when the table is altered by the statement.
        	PreparedStatement prepStat = conn.prepareStatement("INSERT INTO " + dbName + ".records VALUES(default, ?, ?, ?, ?, ?)");
            //statement.executeUpdate("INSERT INTO " + dbName + ".records VALUES(default, '" +
            //    text + "', " + patientID + ", " + personID + ", " + nurseID + ", " +
            //    divisionID + ");");
        	prepStat.setString(1, text);
        	prepStat.setInt(2, patientID);
        	prepStat.setInt(3, personID);
        	prepStat.setInt(4, nurseID);
        	prepStat.setInt(5, divisionID);
        	prepStat.executeUpdate();
        	//Get the ID of the new record.
        	prepStat = conn.prepareStatement("SELECT id FROM " + dbName + ".records ORDER BY id desc LIMIT 1");
        	resultSet = prepStat.executeQuery();
            //resultSet = statement.executeQuery("SELECT id FROM " + dbName + ".records ORDER BY id desc LIMIT 1");
            resultSet.next();
            newID = resultSet.getInt(1);
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        return newID;
    }

	public void update(int recordID, String newText, Connection conn, ResultSet resultSet) {
		try {
			//executeUpdate() is used when the table is altered by the statement.
			preparedStatement = conn.prepareStatement("UPDATE " + dbName + ".records SET text = ? WHERE id = ?");
			preparedStatement.setString(1, newText);
			preparedStatement.setInt(2, recordID);
			preparedStatement.executeUpdate();
			//statement.executeUpdate("UPDATE " + dbName + ".records SET text = '" + newText
			//	+ "' WHERE id = " + recordID);
		} catch (SQLException ex) {

			System.out.println("SQLException i update(): " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
		}
	}

	public void delete(int recordID, Connection conn, ResultSet resultSet) {
		try {
			//executeUpdate() is used when the table is altered by the statement.
			preparedStatement = conn.prepareStatement("DELETE FROM " + dbName + ".records WHERE id = ?");
			preparedStatement.setInt(1, recordID);
			preparedStatement.executeUpdate();
			//statement.executeUpdate("DELETE FROM " + dbName + ".records WHERE id = " + recordID);
		} catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
		}
	}
	public static int newPerson(String personName, int roleID, int divisionID, Connection conn, ResultSet resultSet){
		int id = -1;
		try {
			PreparedStatement prepStat = conn.prepareStatement("INSERT INTO " + dbName + ".persons VALUES(default, ?, ?, ?)");
			prepStat.setString(1, personName);
			prepStat.setInt(2, roleID);
			prepStat.setInt(3, divisionID);
			prepStat.executeUpdate();
			//statement.executeUpdate("INSERT INTO " + dbName + ".persons VALUES(default, '" + personName + "', " + roleID + ", " + divisionID + ");");
			//Check id of new patient.
			prepStat = conn.prepareStatement("SELECT id FROM " + dbName + ".persons ORDER BY id desc LIMIT 1");
			resultSet = prepStat.executeQuery();
			//resultSet = statement.executeQuery("SELECT id FROM " + dbName + ".persons ORDER BY id desc LIMIT 1");
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
