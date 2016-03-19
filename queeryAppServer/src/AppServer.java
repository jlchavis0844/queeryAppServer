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

	private Socket csocket;//socket to connect to the client
	private Thread mainT;//thread that runs the socket
	private BufferedReader read;//reads incoming text
	private PrintStream out;//sends return text
	private Boolean killStatus;//kills server when set to false
	private List <String> commandList;//list text string
	//private String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	private static final String DB_URL = "jdbc:mysql://cecs-db01.coe.csulb.edu/cecs491bp";
	private static final String USER = "cecs491a11";
	private static final String PASS = "ohChox";//hard coded password
	private Connection conn = null; //create connection
	private Statement stmt = null; //create null statement
	private ResultSet rs = null;//for return results
	private Boolean error = false;//if an error is found
	String command;//holds SQL command
	
	/**
	 * default constructor
	 * @param csocket - socket to connect to 
	 * @throws IOException
	 */
	AppServer(Socket csocket) throws IOException {
		killStatus= true;//kills server when set to false( FALSE = BYE BYE)
		this.csocket = csocket;//receive socket
		mainT = new Thread(this);//make this a new thread
		read = new BufferedReader(new InputStreamReader(csocket.getInputStream()));//new reader
		out = new PrintStream(csocket.getOutputStream());//new writer
		try {
			Class.forName("com.mysql.jdbc.Driver");//load JDBC driver
			conn = DriverManager.getConnection(DB_URL,USER,PASS);//complete connection
			stmt = conn.createStatement();//set statement from DB connection
			rs = null;//holds results
			command = read.readLine();//read in the command
			//start command parse
			commandList = Arrays.asList(command.split("\\s*,\\s*"));//load text command into a list
		} catch (ClassNotFoundException e) {
			// JDBC error
			e.printStackTrace();//print to console
			out.println(e.getMessage());// send error back to client
			error = true;//set internal error status
		} catch (SQLException e) {
			//they made me put this in here
			out.println(e.getMessage());
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
		for(String s: commandList){
			System.out.print(s + "\t");
		}
		System.out.println("\nEnd command");
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
		try {
			read.close();//close bufffers
			out.close();//close buffers
		} catch (IOException e) {
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
	private void addUser(){
		try{
			System.out.println("Adding a new user");
			
			for(int i = 0; i < commandList.size(); i++){//go through command list
				System.out.print(commandList.get(i) + "\t");//print out the command and params
			}
			System.out.println();
			
			//build SQL statement
	        String sqlCmd = "INSERT INTO users VALUES ('" + commandList.get(1) + "', '"
						+ commandList.get(2) + "', '" + commandList.get(3) + "', '"
						+ commandList.get(4) + "', '" + commandList.get(5) + "', '"
						+ commandList.get(6) + "');";
	        System.out.println("sending command:" + sqlCmd);//print SQL command to console
	        stmt.executeUpdate(sqlCmd);//send command
	        
		} catch(SQLException se){
			//Handle errors for JDBC
			error = true;
		    se.printStackTrace();
		    out.println(se.getMessage());//return error message
		} catch(Exception e){
			//general error case, Class.forName
			error = true;
			e.printStackTrace();
			out.println(e.getMessage());
		} finally{
		      //finally block used to close resources
		      try{//close SQLQuery
		         if(stmt!=null)
		            stmt.close();
		      }catch(SQLException se2){
		    	  System.out.println("Can't close statement");
		    	  out.println(se2.getMessage());
		      }// nothing we can do
		      try{//close the connection
		         if(conn!=null)
		            conn.close();
		      }catch(SQLException se){
		    	 System.out.println("Can't close conn");
		         se.printStackTrace();
		         out.println(se.getMessage());
		      }//end finally try
		      if(error == false)//if an exception, is not thrown, returns true
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
		String sqlCmd = "SELECT password FROM users WHERE userName = \"" + commandList.get(1) + "\";";
		try {//being SQL command
			rs = stmt.executeQuery(sqlCmd);//execute SQL, store result in rs
			if(rs == null){//if the rs is null, no matching userName was found
				out.println("userNotFound");//send client the error message
			} else if (rs.next()){
				String tPass = rs.getString("password");//store returned password
				if(tPass.equals(commandList.get(2))){//compare given and the returned
					System.out.println("password match");
					out.println("true");//return match to the client
				} else{
					error = true;
					out.println("pswdError");//passwords did not match
				}
			}
		} catch (SQLException e) {//connection error
			error = true;
			out.println(e.getMessage());
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
			System.out.println("sliderError");//write the error code to the client
			out.println("");
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
		//make the sql command
		String sqlCmd = "INSERT INTO location VALUES ('" + commandList.get(1) + "', '"
						+ commandList.get(2) + "', '" + commandList.get(3) + "');";
		
		try {//start SQL statement
			stmt.executeUpdate(sqlCmd);
			
		} catch (SQLException e) {
			error = true;
			out.println(e.getMessage());
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
			out.println(e.getMessage());
			e.printStackTrace();
		}
		if(error == false)
			out.println("true");
	}

	/**
	 * Finds and returns matches based on parameters listed below
	 * 0 - getMatches
	 * 1 - userName
	 * 2 - searching user's longitude
	 * 3 - searching user's latitude
	 * 4 - searching user's pGender min
	 * 5 - searching user's pGender max
	 * 6 - searching user's pExpression min
	 * 7 - searching user's pExpression max
	 * 8 - searching user's pOrientation min
	 * 9 - searching user's pOrientation max
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
		ArrayList<String> matchList = new ArrayList<>();//holds user names of found matches
		
		String command = "SELECT pGender, pExpression, pOrientation "
				+ "FROM personalSlider WHERE userName = " + commandList.get(1);
		
		//load in SQL command
		command ="SELECT location.userName" + "FROM (location INNER JOIN users"
				+ "ON location.userName = users.userName) INNER JOIN personalSlider "
				+ "ON users.userName = personalSlider.userName WHERE (((location.longitude) <= ";
		command += roundLongUp(commandList.get(2)) + "And (location.longitude) >= ";
		command += roundLongDown(commandList.get(2)) + ") AND ((location.latitude) <= ";
		command += roundLatUp(commandList.get(3)) + " And (location.latitude)>= ";
		command += roundLatDown(commandList.get(3)) + ") AND ((personalSlider.pGender)>=";
		command += commandList.get(4) + " And (personalSlider.pGender)<= " + commandList.get(5);
		command += "AND ((personalSlider.pExpression)>= " + commandList.get(6) +
				" And (personalSlider.pExpression)<= " + commandList.get(7) + ")";
		command += "AND ((personalSlider.pOrientation)>=" + commandList.get(8) +
				" And (personalSlider.pOrientation)<=" + commandList.get(9) + "));";
		
		try {//try block for sending SQL command
			rs = stmt.executeQuery(command);//send command
			
			while(rs.next()){//while there are matches
				matchList.add(rs.getString("userName"));//load userName
			}
			
			//each String[] is a match, 0 = userName, 1 = pGenderMin, .....6=pOrientationMax
			ArrayList<String[]> fullMatches = getSeekingSlider(matchList);
			
			//remove the non-overlapping users
			//crossMatch(commandList, fullMatches);
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			out.println(e.getMessage());
		}
		
	}
	
	/**
	 * Rounds number up to three decimal places at adds 0.4 to number
	 * @param longitude - String of the longitude
	 * @return String of the new longitude
	 */
	private String roundLongUp(String longitude){
		double temp = new BigDecimal(Double.valueOf(longitude))
			    .setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
		temp += 0.4;
		
		return Double.toString(temp);
	}
	
	/**
	 * Rounds number up to three decimal places at subtracts 0.4 to number
	 * @param longitude - String of the longitude
	 * @return String of the new longitude
	 */
	private String roundLongDown(String longitude){
		double temp = new BigDecimal(Double.valueOf(longitude))
			    .setScale(3, BigDecimal.ROUND_HALF_UP)
			    .doubleValue();
		temp -= 0.4;
		
		return Double.toString(temp);
	}
	
	/**
	 * Rounds number up to three decimal places at adds 0.4 to number
	 * @param lat - String of the latitude
	 * @return String of the new latitude
	 */
	private String roundLatUp(String lat){
		double temp = new BigDecimal(Double.valueOf(lat))
			    .setScale(3, BigDecimal.ROUND_HALF_UP)
			    .doubleValue();
		temp += 0.5;
		
		return Double.toString(temp);
	}
	
	/**
	 * Rounds number up to three decimal places at adds 0.4 to number
	 * @param lat - String of the latitude
	 * @return String of the new latitude
	 */
	private String roundLatDown(String longitude){
		double temp = new BigDecimal(Double.valueOf(longitude))
			    .setScale(3, BigDecimal.ROUND_HALF_UP)
			    .doubleValue();
		temp -= 0.5;
		
		return Double.toString(temp);
	}
	
	/**
	 * fetches the seeking slider information per userName and returns it in an ArrayList
	 * @param inList - ArrayList<String> of the userNames to fetch info for.
	 * @return ArrayList<String> holding userName, gender min, gendermax, expressionmin,
	 * expressionmax, orientationmin, orientationmax
	 */
	private ArrayList<String[]> getSeekingSlider(ArrayList<String> inList){
		String command = "SELECT * FROM seekingSlider WHERE userName = ";
		ArrayList<String[]> tempList = new ArrayList<>();
		int listSize = inList.size();
		String[] strArr = new String[7];//holds matches data
		
		if(listSize == 0) return tempList;//if the list is empty, return empty list
		
		if(listSize == 1){//if there is only 1 in the list
			command += inList.get(0) + ";";
		} else {//if there is more than 1
			for(int i = 0; i < listSize; i++){//go through the list
				if(i == 0){//for the first userName
					command += inList.get(i);
				} else if(i == listSize){//for the last user name
					command += " or WHERE userName = " + inList.get(i) + ";";
				} else {//for all other userNames
					command += " or WHERE userName = " + inList.get(i);
				}
			}//end for loop
		}
		try {//send command and parse results
			rs = stmt.executeQuery(command);
			
			while(rs.next()){//while there is a result remaining
				//get result and store it into a string
				/*String temp = rs.getString(0) + "," + rs.getString(1) + ","  + rs.getString(2)
						 + "," + rs.getString(3) + "," + rs.getString(4) + "," + rs.getString(5)
						 + "," + rs.getString(6);
				tempList.add(temp);//add string to the tempList*/
				tempList.add(new String[]{rs.getString(0),rs.getString(1),rs.getString(2),
						rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6)});
			}//end while loop
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//out.println(e.getErrorCode());
		}
		return tempList;
	}
	
	private void crossMatch(String client, ArrayList<String[]> matchList){
		
	}
	
}//end class