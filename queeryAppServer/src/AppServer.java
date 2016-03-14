import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.sql.*;

/**
 * class that is created when a server connection is made
 * takes command from socket and parses command
 * runs SQL tasks based on command name from socket.readLine()
 * @author James
 *
 */
public class AppServer implements Runnable{

	private Socket csocket;
	private Thread mainT;
	private BufferedReader read;
	private PrintStream out;
	private Boolean killStatus;//kills server when set to false
	private List <String> commandList;
	//private String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	private static final String DB_URL = "jdbc:mysql://cecs-db01.coe.csulb.edu/cecs491bp";
	private static final String USER = "cecs491a11";
	private static final String PASS = "ohChox";//hard coded password
	private Connection conn = null; //create connection
	private Statement stmt = null; //create null statement
	private ResultSet rs = null;//for return results
	private Boolean error = false;
	private String command;//holds the command to be completed
	
	/**
	 * default constructor
	 * @param csocket - socket to connect to 
	 * @throws IOException
	 */
	AppServer(Socket csocket) throws IOException {
		killStatus= true;//kills server when set to false( FALSE = BYE BYE)
		this.csocket = csocket;
		mainT = new Thread(this);
		read = new BufferedReader(new InputStreamReader(csocket.getInputStream()));
		out = new PrintStream(csocket.getOutputStream());
		try {
			Class.forName("com.mysql.jdbc.Driver");//load JDBC driver
			conn = DriverManager.getConnection(DB_URL,USER,PASS);//complete connection
			stmt = conn.createStatement();//set statement from DB connection
			rs = null;//holds results
			command = read.readLine();//read in the command
			//start command parse
			commandList = Arrays.asList(command.split("\\s*,\\s*"));
		} catch (ClassNotFoundException e) {
			// JDBC error
			e.printStackTrace();//print to console
			out.println("JDBCerror");// send error back to client
			error = true;//set internal error status
		} catch (SQLException e) {
			//they made me put this in here
			out.println("connError");
			error = true;
			e.printStackTrace();
		}

		mainT.start();//start the main thread
		
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {//start thread functions on incoming command
		
		//switch to read command
		switch(commandList.get(0)){
			case "kill"://this is a method for shutting down the server
				killStatus = true;//it is checked by the server, kills server on true
				break;
		
			case "addUser"://for adding a new user
				addUser();//calls method for adding a new user
				break;
				
			case "login"://client attempts to log in a user
				login();
				break;
				
			case "setPSlider"://called on registration
				setPersonalSlider();//INSERT
				break;
			
			case "updatePSlider":
				updatePersonalSlider();//UPDATE
				break;
				
			case "setLocation":
				setLocation();//called on registration only, sets location 
				break;
				
			case "updateLocation":
				updateLocation();//updates a user's location
				break;
				
			default: 
				System.exit(2);//crash and burn
				break;
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
	private void addUser(){
		try{
			System.out.println("Adding a new user");
			
			for(int i = 0; i < commandList.size(); i++){
				System.out.print(commandList.get(i) + "\t");
			}
			System.out.println();
			
			//build SQL statement
	        String sqlCmd = "INSERT INTO users VALUES ('" + commandList.get(1) + "', '"
						+ commandList.get(2) + "', '" + commandList.get(3) + "', '"
						+ commandList.get(4) + "', '" + commandList.get(5) + "', '"
						+ commandList.get(6) + "');";
	        System.out.println("sending command:" + sqlCmd);
	        stmt.executeUpdate(sqlCmd);//send command
	        
		} catch(SQLException se){
			//Handle errors for JDBC
			error = true;
		    se.printStackTrace();
		    out.println("SQLerror");
		} catch(Exception e){
			//general error case, Class.forName
			error = true;
			e.printStackTrace();
			out.println("genError");
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
		      if(error = false)//if an exception, is not thrown, returns true
		    	  out.println("true");//return true
		   }//end try	
		}
	
	/**
	 * used to end the server running
	 * server runs until this killStatus is set to false
	 * killStatus changed in switch case during command parse
	 * @return killStatus - true/false of whether to shutdown the server
	 */
	public Boolean getServerStatus(){
		return killStatus;
	}
	
	/**
	 * used to check that the client user login info is correct
	 * 
	 * 0 - login
	 * 1 - userName
	 * 2 - password
	 * 
	 * returns true on login, false otherwise
	 * 
	 */
	private void login(){
		//SELECT password FROM users WHERE userName = userName
		String sqlCmd = "SELECT password FROM users WHERE userName = "
						+ commandList.get(1);
		try {//being SQL command
			rs = stmt.executeQuery(sqlCmd);//execute SQL, store result in rs
			if(rs == null){//if the rs is null, no matching userName was found
				out.println("userNotFound");//send client the error message
			} else {
				String tPass = rs.getString("password");//store returned password
				if(tPass == commandList.get(1)){//compare given and the returned
					out.println("true");//return match to the client
				} else{
					out.println("pswdError");//passwords did not match
				}
			}
		} catch (SQLException e) {//connection error
			error = true;
			out.println("connError");
			e.printStackTrace();
		}
	}
	
	/**
	 * The method to process the set personal slider command
	 * 0 - setSlider
	 * 1 - userName
	 * 2 - gender identity
	 * 3 - gender expression
	 * 4 - sexual orientation
	 * 
	 * sends client "true" if the slider is set, error on fail
	 */
	private void setPersonalSlider(){
		//INSERT INTO personalSlider values ('userName', 'int', 'int', 'int');
		String sqlCmd = "INSERT INTO personalSlider VALUES ('" + commandList.get(1)
						+ "', '" + commandList.get(2) + "', '" + commandList.get(3)
						+ "', '" + commandList.get(4) + "');";
		try {//send the SQL command
			stmt.executeUpdate(sqlCmd);//send command
		} catch (SQLException e) {
			error = true;//set error condition
			out.println("sliderError");//write the error code to the client
			e.printStackTrace();//send  to console
		}
		if (error = false)//if there is no error caught
			out.println("true");//send success to the client
	}
	
	/**
	 * The method to process the update personal slider command
	 * 0 - setSlider
	 * 1 - userName
	 * 2 - gender identity
	 * 3 - gender expression
	 * 4 - sexual orientation
	 * 
	 * sends client "true" if the slider is set, error on fail
	 */
	private void updatePersonalSlider(){
		//UPDATE personalSlider SET pGender='int', pExpression='int', pOrientation='int' WHERE userName='userName';
		String sqlCmd = "UPDATE personalSlider SET pGender = '" + commandList.get(2) + "', pExpression = '"
						+ commandList.get(3) + "', pOrientation = '" + commandList.get(4)
						+ "' WHERE userName = '" + commandList.get(1) + "';";
		try {//send the SQL command
			stmt.executeUpdate(sqlCmd);//send command
		} catch (SQLException e) {
			error = true;//set error condition
			out.println("sliderError");//write the error code to the client
			e.printStackTrace();//send  to console
		}
		if (error = false)//if there is no error caught
			out.println("true");//send success to the client
	}

	/**
	 * sets the location on the user's registration
	 * 
	 * 0 - setLocation
	 * 1 - userName
	 * 2 - raw longitude
	 * 3 - raw latitude
	 */	
	private void setLocation(){
		String sqlCmd = "INSERT INTO location VALUES ('" + commandList.get(1) + "', '"
						+ commandList.get(2) + "', '" + commandList.get(3) + "');";
		
		try {//start SQL statement
			stmt.executeUpdate(sqlCmd);
			
		} catch (SQLException e) {
			error = true;
			out.println("locError");
			e.printStackTrace();
		}
		if(error == false)
			out.println("true");
	}


	/**
	 * updates the user's current location
	 * 
	 * 0 - setLocation
	 * 1 - userName
	 * 2 - raw longitude
	 * 3 - raw latitude
	 */	
	private void updateLocation(){
		//UPDATE location SET longitude = 'double', latitude 'double' WHERE userName = userName
		String sqlCmd = "UPDATE location SET longitude = '" + commandList.get(2) + "', latitude = '"
						+ commandList.get(3) + "', WHERE userName = '" + commandList.get(1) + "';";
		
		try {//start SQL statement
			stmt.executeUpdate(sqlCmd);
			
		} catch (SQLException e) {
			error = true;
			out.println("locError");
			e.printStackTrace();
		}
		if(error == false)
			out.println("true");
	}

	/**
	 * Finds and returns matches based on parameters listed below
	 * 0 - getMatches
	 * 1 - searching user's longitude
	 * 2 - searching user's latitude
	 * 3 - searching user's pGender min
	 * 4 - searching user's pGender max
	 * 5 - searching user's pExpression min
	 * 6 - searching user's pExpression max
	 * 7 - searching user's pOrientation min
	 * 8 - searching user's pOrientation max
	 * 
	 *SELECT location.userName FROM (location INNER JOIN users ON location.userName = users.userName)
	 *INNER JOIN personalSlider ON users.userName = personalSlider.userName
	 *WHERE (((location.longitude)<35 And (location.longitude)>22) 
	 *AND ((location.latitude)<117 And (location.latitude)>-120)
	 *AND ((personalSlider.pGender)>=0 And (personalSlider.pGender)<=10)- done
	 *AND ((personalSlider.pExpression)>=0 And (personalSlider.pExpression)<=5)- done
	 *AND ((personalSlider.pOrientation)>=0 And (personalSlider.pOrientation)<=5));
	 */
	private void getMatches(){
		ArrayList<String> matchList = new ArrayList<>();
		
		String command ="SELECT location.userName" + "FROM (location INNER JOIN users"
				+ "ON location.userName = users.userName) INNER JOIN personalSlider "
				+ "ON users.userName = personalSlider.userName WHERE (((location.longitude) <= ";
		command += roundLongUp(commandList.get(1)) + "And (location.longitude) >= ";
		command += roundLongDown(commandList.get(1)) + ") AND ((location.latitude) <= ";
		command += roundLatUp(commandList.get(2)) + " And (location.latitude)>= ";
		command += roundLatDown(commandList.get(2)) + ") AND ((personalSlider.pGender)>=";
		command += commandList.get(3) + " And (personalSlider.pGender)<= " + commandList.get(4);
		command += "AND ((personalSlider.pExpression)>= " + commandList.get(5) +
				" And (personalSlider.pExpression)<= " + commandList.get(6) + ")";
		command += "AND ((personalSlider.pOrientation)>=" + commandList.get(7) +
				" And (personalSlider.pOrientation)<=" + commandList.get(8) + "));";
		
		try {
			rs = stmt.executeQuery(command);
			
			while(rs.next()){
				matchList.add(rs.getString("userName"));
			}
			
			matchList = getSeekingSlider(matchList);
			
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private String roundLongUp(String longitude){
		double temp = new BigDecimal(Double.valueOf(longitude))
			    .setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
		temp += 0.4;
		
		return Double.toString(temp);
	}
	
	private String roundLongDown(String longitude){
		double temp = new BigDecimal(Double.valueOf(longitude))
			    .setScale(3, BigDecimal.ROUND_HALF_UP)
			    .doubleValue();
		temp -= 0.4;
		
		return Double.toString(temp);
	}
	
	private String roundLatUp(String lat){
		double temp = new BigDecimal(Double.valueOf(lat))
			    .setScale(3, BigDecimal.ROUND_HALF_UP)
			    .doubleValue();
		temp += 0.5;
		
		return Double.toString(temp);
	}
	
	private String roundLatDown(String longitude){
		double temp = new BigDecimal(Double.valueOf(longitude))
			    .setScale(3, BigDecimal.ROUND_HALF_UP)
			    .doubleValue();
		temp -= 0.5;
		
		return Double.toString(temp);
	}
	
	private ArrayList<String> getSeekingSlider(ArrayList<String> inList){
		String command = "SELECT * FROM seekingSlider WHERE userName = ";
		ArrayList<String> tempList = new ArrayList<>();
		int listSize = inList.size();

		if(listSize == 0) return inList;
		
		for(int i = 0; i < listSize; i++){
			if(i == 0){
				command += inList.get(i);
			} else if(i == listSize){
				command += " or WHERE userName = " + inList.get(i) + ";";
			} else {
				command += " or WHERE userName = " + inList.get(i);
			}
		}//end for loop
		try {
			rs = stmt.executeQuery(command);
			
			while(rs.next()){
				String temp = rs.getString(0) + "," + rs.getString(1) + ","  + rs.getString(2)
						 + "," + rs.getString(3) + "," + rs.getString(4) + "," + rs.getString(5)
						 + "," + rs.getString(6);
				tempList.add(temp);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//out.println(e.getErrorCode());
		}
		return tempList;
	}


}//end class