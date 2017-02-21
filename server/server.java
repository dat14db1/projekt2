import java.io.*;
import java.net.*;
import java.security.KeyStore;
import javax.net.*;
import javax.net.ssl.*;
import javax.security.cert.X509Certificate;
import java.util.ArrayList;



public class server implements Runnable {
    private ServerSocket serverSocket = null;
    private static int numConnectedClients = 0;
    private int personID = 1;
    private int NURSE = 2;
    private int PATIENT = 3;
    private int DOCTOR = 1;
    private int GOVERNMENT = 4;

    public server(ServerSocket ss) throws IOException {
        serverSocket = ss;
        newListener();
    }

    public void run() {
        try {
            SSLSocket socket=(SSLSocket)serverSocket.accept();
            newListener();
            SSLSession session = socket.getSession();
            X509Certificate cert = (X509Certificate)session.getPeerCertificateChain()[0];
            String subject = cert.getSubjectDN().getName();
            try {
                String setuptemp = cert.getSubjectDN().getName();
                personID = Integer.parseInt(setuptemp.split("[,=\\s]")[1]);
            } catch (Exception e) {
                System.out.println("Wrong certificate format.");
            }
    	    numConnectedClients++;
            System.out.println("client connected");
            System.out.println("client name (cert subject DN field): " + subject);
            //---Own code starts here---
            //Print issuer:
            String issuer = cert.getIssuerDN().getName();
            System.out.println("certificate issuer on certificate received from server:\n" + issuer + "\n");
            //Print serial number:
            String serialNumber = cert.getSerialNumber().toString();
            System.out.println("certificate serial number on certificate received from server:\n" + serialNumber + "\n");
            //---Own code ends here---
            System.out.println(numConnectedClients + " concurrent connection(s)\n");

            // Setup and send Welcome message
            SQLTest sqlTest = new SQLTest();
            ArrayList<Record> records = sqlTest.listRecords(personID);
            int nolines = records.size() + 4;
            String person_role = null;
            System.out.println("personID is " + personID);
            if (sqlTest.checkPersonRole(personID, DOCTOR)) {
                person_role = "DOCTOR";
            } else if (sqlTest.checkPersonRole(personID, NURSE)) {
                person_role = "NURSE";
            } else if (sqlTest.checkPersonRole(personID, PATIENT)) {
                person_role = "PATIENT";
            } else if (sqlTest.checkPersonRole(personID, GOVERNMENT)) {
                person_role = "GOVERNMENT";
            }
            String personName = sqlTest.getPersonName(personID);
            if (personName == null) {
                personName = "";
            }
            StringBuilder welcome_message = new StringBuilder(nolines + "\nWelcome " + personName + "! You are logged in as " + person_role + ". Write 'help' for options \n \n");

            welcome_message.append("You have permission to read the following patient records: \n");

            for(Record rec : records){
                welcome_message.append(rec.id + ": " + rec.patientName + "\n");
            }

            PrintWriter out = null;
            BufferedReader in = null;
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(welcome_message.toString());
            //System.out.println(welcome_message.toString());
            out.flush();

            String clientMsg = null;
            StringBuilder answer_message = new StringBuilder();
            StringBuilder text = null; // text for create function
            Record temp = null;
            while ((clientMsg = in.readLine()) != null) {
                //Handle request
                //Put words in array

                String[] words = clientMsg.split("[\\n\\s]");//splits on everything but alphanumerical characters
                System.out.println(words[0]);
                answer_message.setLength(0);
                switch (words[0]) {
                    case "list":
                        //System.out.println("list called.");
                        records.clear();
                        records = sqlTest.listRecords(personID);
                        nolines = records.size() + 1;
                        answer_message.append(nolines + "\n");
                        for(Record rec : records){
                            answer_message.append(rec.id + ": " + rec.patientName + "\n");
                        }
                        break;
                    case "read":
                        //Make sure there is a second input arg, that is an integer
                        if (words.length < 2) {
                            answer_message.append(2 + "\nInvalid. Read needs <record_id> as input.\n");
                            break;
                        }
                        try {
                            if (words.length >= 2) {
                                Integer.parseInt(words[1]);
                            }
                        } catch (Exception e){
                            answer_message.append(2 + "\nInvalid. Read needs <record_id> as input.\n");
                            break;
                        }
                        System.out.println("read called on " + words[1]);
                        temp = sqlTest.readRecord(Integer.parseInt(words[1]), personID);
                        if (temp == null) {
                            answer_message.append("2\nUnable to fetch record.\n");
                        } else {
                            nolines = 6 + temp.text.split("\n").length;
                            answer_message.append(nolines + "\nRecordID: " + temp.id + "\n");
                            answer_message.append("Patient: " + temp.patientName + "\n");
                            answer_message.append("Doctor: " + temp.doctorName + "\n");
                            answer_message.append("Nurse: " + temp.nurseName + "\n");
                            answer_message.append("Division: " + temp.divisionName + "\n");
                            answer_message.append(temp.text + "\n");
                        }
                        break;
                    case "delete":
                        //Make sure there is a second input arg, that is an integer
                        if (words.length < 2) {
                            answer_message.append(2 + "\nInvalid. Delete needs <record_id> as input.\n");
                            break;
                        }
                        try {
                            Integer.parseInt(words[1]);
                        } catch (Exception e){
                            answer_message.append(2 + "\nInvalid. Delete needs <record id> as input.\n");
                            break;
                        }
                        System.out.println("delete called on " + words[1]);
                        temp = sqlTest.deleteRecord(Integer.parseInt(words[1]), personID);
                        if (temp == null) {
                            answer_message.append(2 + "\nUnable to delete record.\n");
                        } else {
                            answer_message.append(2 + "\nDeleted record with id " + temp.id + "\n");
                        }
                        break;
                    case "create":
                        int reqnurse, reqpatient, reqdivision;
                        if (words.length < 5) {
                            answer_message.append(2 + "\nInvalid. Create needs inputs <nurse-id> <patient-id> <divisions-id> <journaltext>.\n");
                            break;
                        }
                        try {
                            reqnurse = Integer.parseInt(words[1]);
                            reqpatient = Integer.parseInt(words[2]);
                            reqdivision = Integer.parseInt(words[3]);
                        } catch (Exception e){
                            answer_message.append(2 + "\nInvalid. Create needs inputs <nurse-id> <patient-id> <divisions-id> <journaltext>.\n");
                            break;
                        }
                        //check if valid nurse & patient
                        if (!(checkRole(reqnurse, NURSE, sqlTest))) {
                            answer_message.append(2 + "\nInvalid, nonexisting nurse, nurseID: " + reqnurse + "\n");
                            break;
                        }
                        if (!(checkRole(reqpatient, PATIENT, sqlTest))) {
                            answer_message.append(2 + "\nInvalid, nonexisting patient, patientID: " + reqpatient + "\n");
                            break;
                        }
                        if (!(checkDivExist(reqdivision, sqlTest))) {
                            answer_message.append(2 + "\nInvalid, nonexisting division, divID: " + reqdivision + "\n");
                            break;
                        }

                        text = new StringBuilder();
                        System.out.printf(2 + "\nCreate called with nurseID %s, patientID %s, divisionID %s, and journaltext start with %s \n", words[1], words[2], words[3], words[4]);
                        for (int i = 4; i < words.length; i++){
                            System.out.println(words[i]);
                            text.append(words[i] + " ");
                        }
                        answer_message.append(2 + "\nCreate with valid arguments. New record created.\n");

                        temp = sqlTest.createRecord(personID, Integer.parseInt(words[1]),Integer.parseInt(words[2]),Integer.parseInt(words[3]), text.toString());
                    break;
                    case "newpatient": // division, name
                        if (words.length < 3) {
                            answer_message.append(2 + "\nInvalid. Newpatient needs inputs <division> <name>\n");
                            break;
                        }
                        int divisionID;
                        try {
                            divisionID = Integer.parseInt(words[1]);
                        } catch (Exception e){
                            answer_message.append(2 + "\nInvalid. Newpatient needs inputs <division> <name>\n");
                            break;
                        }
                        text = new StringBuilder();
                        for(int i = 2; i <words.length; i++){
                            text.append(words[i] + " ");
                        }
                        String name = text.toString();
                        int id = sqlTest.createPerson(personID, name, PATIENT, divisionID);
                        if (id == -1) {
                            answer_message.append(2 + "\nCreate permission denied.\n");
                        } else {
                            answer_message.append(2 + "\nPatient " + name + " created with ID " + id + ".\n");
                        }
                        break;
                    case "update": // update, reportNO, text
                        if (words.length < 3) {
                            answer_message.append(2 + "\nInvalid. Update needs inputs <int reportNO> <newText>\n");
                            break;
                        }
                        int recordID;
                        try {
                            recordID = Integer.parseInt(words[1]);
                        } catch (Exception e){
                            answer_message.append(2 + "\nInvalid. Update needs inputs <int reportNO> <newText>\n");
                            break;
                        }
                        text = new StringBuilder();
                        for (int i = 2; i < words.length; i++) {
                            text.append(words[i] + " ");
                        }
                        temp = sqlTest.updateRecord(recordID, personID, text.toString());
                        if (temp == null) {
                            answer_message.append(2 + "\nUpdate Permission denied\n");
                            break;
                        }
                        answer_message.append(2 + "\nRecord successfully updated\n");
                        break;
                        case "help":
                            answer_message.append(stateOptions());
                        break;
                    case "listpersons":
                        //Return a list with all staff and patients together with their id:s.
                        //Only doctors are allowed to do this operation.
                        ArrayList<String> list = sqlTest.listPersons(personID);
                        if (list == null) {//Operation not allowed.
                            answer_message.append(2 + "\nOnly doctors may list persons.\n");
                        } else {
                            nolines = list.size() + 1;
                            answer_message.append(nolines + "\n");
                            for (String s : list) {
                                answer_message.append(s + "\n");
                            }
                        }
                        break;
                    case "listdivisions":
                        //Return a list with all staff and patients together with their id:s.
                        //Only doctors are allowed to do this operation.
                        ArrayList<String> divList = sqlTest.listDivisions(personID);
                        if (divList == null) {//Fetching of list failed.
                            answer_message.append(2 + "\nError.\n");
                        } else {
                            nolines = divList.size() + 1;
                            answer_message.append(nolines + "\n");
                            for (String s : divList) {
                                answer_message.append(s + "\n");
                            }
                        }
                        break;
                    default:
                        answer_message.append(2 + "\nInvalid operation.\n");
                        System.out.println("client msg: " + clientMsg);
                        break;
                }
			    //String rev = new StringBuilder(clientMsg).reverse().toString();
                System.out.println("received '\n" + clientMsg + "\n' from client");
                System.out.print("sending '" + answer_message.toString() + "' to client...");
				out.println(answer_message.toString());
				out.flush();
                System.out.println("done\n");
			}
			in.close();
			out.close();
			socket.close();
    	    numConnectedClients--;
            System.out.println("client disconnected");
            System.out.println(numConnectedClients + " concurrent connection(s)\n");
		} catch (IOException e) {
            System.out.println("Client died: " + e.getMessage());
            e.printStackTrace();
            return;
        }
    }

    private void newListener() { (new Thread(this)).start(); } // calls run()

    private boolean checkRole(int personID, int role, SQLTest sqlTest){
        return sqlTest.checkPersonRole(personID, role);
    }

    private boolean checkDivExist(int division, SQLTest sqlTest) {
        return sqlTest.checkDivExist(division);
    }

    public static void main(String args[]) {
        System.out.println("\nServer Started\n");
        int port = -1;
        if (args.length >= 1) {
            port = Integer.parseInt(args[0]);
        }
        String type = "TLS";
        try {
            ServerSocketFactory ssf = getServerSocketFactory(type);
            ServerSocket ss = ssf.createServerSocket(port);
            ((SSLServerSocket)ss).setNeedClientAuth(true); // enables client authentication
            new server(ss);
        } catch (IOException e) {
            System.out.println("Unable to start Server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static ServerSocketFactory getServerSocketFactory(String type) {
        if (type.equals("TLS")) {
            SSLServerSocketFactory ssf = null;
            try { // set up key manager to perform server authentication
                SSLContext ctx = SSLContext.getInstance("TLS");
                KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
                KeyStore ks = KeyStore.getInstance("JKS");
				        KeyStore ts = KeyStore.getInstance("JKS");
                char[] password = "password".toCharArray();

                ks.load(new FileInputStream("serverkeystore"), password);  // keystore password (storepass)
                ts.load(new FileInputStream("servertruststore"), password); // truststore password (storepass)
                kmf.init(ks, password); // certificate password (keypass)
                tmf.init(ts);  // possible to use keystore as truststore here
                ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
                ssf = ctx.getServerSocketFactory();
                return ssf;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return ServerSocketFactory.getDefault();
        }
        return null;
    }
    private static String stateOptions(){
        StringBuilder sb = new StringBuilder();
        sb.append(19 + "\n");
        sb.append("*List all records and recordIDs: \n\tlist \n");
        sb.append("*Read a record, list content: \n\tread <record id> \n");
        sb.append("*Delete a record: \n\tdelete <record id> \n");
        sb.append("*Create a new record: \n\tcreate <nurse id> <patient id> <division id> <record text> \n");
        sb.append("*Add new patient to database: \n\tnewpatient <division id> <name>\n");
        sb.append("*Update an existnig record: \n\tupdate <record id> <new record text>\n");
        sb.append("*List all persons: \n\tlistpersons\n");
        sb.append("*List all divisions: \n\tlistdivisions\n");
        sb.append("*List all options: \n\thelp\n");
        return sb.toString();
    }
}
