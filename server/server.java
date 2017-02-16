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
    private int personID = 4;

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

// connected, write to client
            SQLTest sqlTest = new SQLTest(); 
            ArrayList<Record> records = sqlTest.listRecords(personID);
            int nolines = records.size() + 4;
            StringBuilder welcome_message = new StringBuilder(nolines + "\nWelcome, " + subject + "! \n \n");

            welcome_message.append("You have permission to read the following patient records: \n");

            for(Record rec : records){
                welcome_message.append(rec.id + " " + rec.text.substring(0, 3) + "... \n");
            }
            
            // Record temp = sqlTest.readRecord(2, 5);
            // System.out.println("res: " + temp.text);


            PrintWriter out = null;
            BufferedReader in = null;
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(welcome_message.toString());
            System.out.println(welcome_message.toString());
            out.flush();

            String clientMsg = null;
            StringBuilder answer_message = new StringBuilder();
            Record temp = null;
            while ((clientMsg = in.readLine()) != null) {
                //Handle request
                //Put words in array

                String[] words = clientMsg.split("[\\n\\s]");//splits on everything but alphanumerical characters
                System.out.println(words[0]);
                answer_message.setLength(0);
                switch (words[0]) {
                    case "list":
                        System.out.println("list called.");
                        records.clear();
                        records = sqlTest.listRecords(personID);
                        nolines = records.size() + 1;
                        answer_message.append(nolines + "\n");
                        for(Record rec : records){
                            answer_message.append(rec.id + " " + rec.text.substring(0, 3) + "... \n");
                        }
                        break;
                    case "read":
                        //Make sure there is a second input arg, that is an integer
                        if (words.length < 2) {
                            answer_message.append(2 + "\nInvalid. Read needs record id as input.\n");
                            break;
                        }
                        try {
                            if (words.length >= 2) {
                                Integer.parseInt(words[1]);
                            }
                        } catch (Exception e){
                            answer_message.append(2 + "\nInvalid. Read needs record id as input.\n");
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
                            answer_message.append(2 + "\nInvalid. Delete needs record id as input.\n");
                            break;
                        }
                        try {
                            if (words.length >= 2) {
                                Integer.parseInt(words[1]);
                            }
                        } catch (Exception e){
                            answer_message.append(2 + "\nInvalid. Delete needs record id as input.\n");
                            break;
                        }
                        System.out.println("delete called on " + words[1]);
                        temp = sqlTest.deleteRecord(Integer.parseInt(words[1]), personID);
                        if (temp == null) {
                            answer_message.append("2\nUnable to fetch record.\n");
                        } else {
                            answer_message.append("2\nDeleted record with id " + temp.id + "\n");
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
}
