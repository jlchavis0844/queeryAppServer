package queeryAppServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.sql.*;

public class AppServer implements Runnable{

	public static final int PORT = 3306;
	private Socket csocket;
	private Thread mainT;
	private BufferedReader read;
	private PrintStream out;
	//private Boolean on = true;
	private List <String> commandList;
	//private String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	private String DB_URL = "jdbc:mysql://cecs-db01.coe.csulb.edu/cecs491a11";
	private static final String USER = "cecs491a11";//hard coded user
	private static final String PASS = "ohChox";//hard coded password
	private Connection conn = null; //create connection
	private Statement stmt = null; //create null statement
	private ResultSet rs = null;//for return results
	
	/**
	 * default constructor
	 * @param csocket - socket to connect to 
	 * @throws IOException
	 */
	AppServer(Socket csocket) throws IOException {
		this.csocket = csocket;
		mainT = new Thread(this);
		read = new BufferedReader(new InputStreamReader(csocket.getInputStream()));
		out = new PrintStream(csocket.getOutputStream());
		mainT.start();
	}

	@Override
	public void run() {//start thread functions on incoming command
		try {//for the readLine()
			String command = read.readLine();
			//start command parse
			commandList = Arrays.asList(command.split("\\s*,\\s*"));
			
			//switch to read command
			switch(commandList.get(0)){
			case "addUser":
				//addUser Method here
				break;
			default: 
				System.exit(2);//crash and burn
				break;
			}
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}//end run
	
	/**
	 * adds a new user and sends back the userID
	 * commandList should be structured as such
	 * 0 = addUser
	 * 1 = userName
	 * 2 = password
	 * 3 = First
	 * 4 = Last
	 * 5 = email
	 * 6 = age
	 * 
	 * writes status to out, true = success, else error message
	 * builds statements like: INSERT INTO users VALUES
	 * ('HankTankerous', 'password1', 'first', 'last', 'email@mail.get', '34'); 
	 */
	public void addUser(){
		try{
			Class.forName("com.mysql.jdbc.Driver");//load DBC driver
			conn = DriverManager.getConnection(DB_URL,USER,PASS);//complete connection
			stmt = conn.createStatement();//set statement from DB connection
			rs = null;//holds results
			
			//build SQL statement
	        String sqlCmd = "INSERT INTO users VALUES ('" + commandList.get(1) + "', '"
						+ commandList.get(2) + "', '" + commandList.get(3) + "', '"
						+ commandList.get(4) + "', '" + commandList.get(5) + "', '"
						+ commandList.get(6) + "');";
	        stmt.executeUpdate(sqlCmd);//send command
	        
		} catch(SQLException se){
			//Handle errors for JDBC
		    se.printStackTrace();	
		    out.println("JDBC error");
		} catch(Exception e){
			//general error case, Class.forName
			e.printStackTrace();
			out.println("general error");
		} finally{
		      //finally block used to close resources
		      try{
		         if(stmt!=null)
		            stmt.close();
		      }catch(SQLException se2){
		    	  out.println("Can't close statement");
		      }// nothing we can do
		      try{
		         if(conn!=null)
		            conn.close();
		      }catch(SQLException se){
		    	 out.println("Can't close conn");
		         se.printStackTrace();
		      }//end finally try
		      out.println("true");
		   }//end try	
		}
}