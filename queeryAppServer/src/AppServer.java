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
	private BufferedReader read;//reads incoming text
	private PrintStream out;//sends return text
	private List <String> commandList;//list text string
	private static final String DB_URL = "jdbc:mysql://cecs-db01.coe.csulb.edu/cecs491bp?autoReconnect=true&useSSL=false";
	private static final String USER = "cecs491a11";
	private static final String PASS = "ohChox";//hard coded password
	private Connection conn = null; //create connection
	private Statement stmt = null; //create null statement
	private ResultSet rs = null;//for return results
	private Boolean error = false;//if an error is found
	private String command;//holds SQL command
	private int MAX_DISTANCE = 25;

	/**
	 * default constructor
	 * @param csocket - socket to connect to 
	 * @throws IOException
	 */
	AppServer(Socket csocket, Boolean serverStatus) {
		this.csocket = csocket;//receive socket
		try {
			read = new BufferedReader(new InputStreamReader(csocket.getInputStream()));//new reader
			out = new PrintStream(csocket.getOutputStream());//new writer
			Class.forName("com.mysql.jdbc.Driver");//load JDBC driver
			conn = DriverManager.getConnection(DB_URL,USER,PASS);//complete connection
			stmt = conn.createStatement();//set statement from DB connection
			rs = null;//holds results

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
		//mainT.start();//start the main thread
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {//start thread functions on incoming command

		try {
			while(read.ready()){
				try {
					stmt = conn.createStatement();//set statement from DB connection
					command = read.readLine();
				} catch (IOException | SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}//read in the command

				//start command parse
				commandList = Arrays.asList(command.split("\\s*,\\s*"));//load text command into a list		
				for(String s: commandList){
					System.out.print(s + " ");
				}

				//switch to read command
				switch(commandList.get(0)){
				case "kill"://this is a method for shutting down the server
					QueeryServer.serverStatus = false;
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

				case "setSSlider":
					setSeekingSlider();
					break;

				case "updateSSlider":
					updateSeekingSlider();
					break;

				case "updateLocation":
					updateLocation();//updates a user's location
					break;
					
				case "getMatches":
					getMatches();
					break;
					
				case "testUserName":
					testUserName();
					break;
					
				case "echo":
					out.println("echo command received");
					break;

				default: 
					System.exit(2);//crash and burn
					break;
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			out.println(e.getMessage());
		} //end try
		try {
			read.close();//close buffers
			out.close();//close buffers
			if(!csocket.isClosed())
				csocket.close();
			if(!stmt.isClosed())
				stmt.close();
			if(!conn.isClosed())
				conn.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}//end run

	/**
	 * adds a new user and sends back the userID. 
	 * commandList should be structured as such<br>
	 * 0 = addUser<br>
	 * 1 = userName<br>
	 * 2 = password<br>
	 * 3 = First<br>
	 * 4 = Last<br>
	 * 5 = email<br>
	 * 6 = age<br>
	 * 
	 * writes status to out, true = success, else error message<br>
	 * builds statements like: INSERT INTO users VALUES
	 * ('HankTankerous', 'password1', 'first', 'last', 'email@mail.get', '34'); <br>
	 * working for proto1
	 * 
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
		} 
		if(error == false)
			out.println("true");//end try
	}

	/**
	 * used to end the server running<br>
	 * server runs until this killStatus is set to false<br>
	 * killStatus changed in switch case during command parse<br>
	 * @return killStatus - true/false of whether to shutdown the server
	 */
	/*public Boolean getServerStatus(){
		return killStatus;
	}*/

	/**
	 * used to check that the client user login info is correct<br>
	 * 
	 * 0 - login<br>
	 * 1 - userName<br>
	 * 2 - password<br>
	 * 
	 * returns true on login, false otherwise<br>
	 * working for Proto1
	 */
	private void login(){
		//SELECT password FROM users WHERE userName = userName
		String sqlCmd = "SELECT password FROM users WHERE userName = \"" + commandList.get(1) + "\";";
		String tPass = null;
		try {//being SQL command
			rs = stmt.executeQuery(sqlCmd);//execute SQL, store result in rs
			if (rs.next()){//if a user was found
				tPass = rs.getString("password");//store returned password
				if(tPass.equals(commandList.get(2))){//compare given and the returned
					System.out.println("password match");
					out.println("true");//return match to the client
				} else{//password mismatch
					error = true;
					out.println("pswdError");//passwords did not match
				}
			} else if(tPass == null) {//if no user is found
				error = true;
				out.println("userNotFound");
			} 
		} catch (SQLException e) {//connection error
			error = true;
			out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * The method to process the set personal slider command<br>
	 * 0 - setSlider<br>
	 * 1 - userName<br>
	 * 2 - gender identity<br>
	 * 3 - gender expression<br>
	 * 4 - sexual orientation<br>
	 * <br>
	 * sends client "true" if the slider is set, error on fail<br>
	 * Working for Proto1
	 * 
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
			out.println(e.getMessage());
			e.printStackTrace();//send  to console
		}
		if (error == false)//if there is no error caught
			out.println("true");//send success to the client
	}

	/**
	 * The method to process the update personal slider command<br>
	 * 0 - setSlider<br>
	 * 1 - userName<br>
	 * 2 - gender identity<br>
	 * 3 - gender expression<br>
	 * 4 - sexual orientation<br>
	 * 
	 * sends client "true" if the slider is set, error on fail
	 * working for proto1
	 */
	private void updatePersonalSlider(){
		//UPDATE personalSlider SET pGender='int', pExpression='int', pOrientation='int' WHERE userName='userName';
		String sqlCmd = "UPDATE personalSlider SET pGender = '" + commandList.get(2) + "', pExpression = '"
				+ commandList.get(3) + "', pOrientation = '" + commandList.get(4)
				+ "' WHERE userName = '" + commandList.get(1) + "';";
		System.out.println(sqlCmd);
		try {//send the SQL command
			int rowsUpdated = stmt.executeUpdate(sqlCmd);//send command

			if(rowsUpdated == 0){//check if user was found (0 = no update)
				out.println("User not found");
				error = true;
			}

			System.out.println(sqlCmd + "\t" + rowsUpdated);
		} catch (SQLException e) {
			error = true;//set error condition
			out.println("sliderError");//write the error code to the client
			e.printStackTrace();//send  to console
		}
		if (error == false)//if there is no error caught
			out.println("true");//send success to the client

	}

	/**
	 * sets the location on the user's registration<br>
	 * 
	 * 0 - setLocation<br>
	 * 1 - userName<br>
	 * 2 - raw longitude<br>
	 * 3 - raw latitude<br>
	 * 
	 * working for proto1
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
	 * updates the user's current location<br>
	 * 
	 * 0 - setLocation<br>
	 * 1 - userName<br>
	 * 2 - raw longitude<br>
	 * 3 - raw latitude<br>
	 * 
	 * working from proto1
	 */	
	private void updateLocation(){
		//UPDATE location SET longitude = double, latitude = double WHERE userName = userName
		String sqlCmd = "UPDATE location SET longitude = " + commandList.get(2) + ", latitude = "
				+ commandList.get(3) + " WHERE userName = '" + commandList.get(1) + "';";
		System.out.println(sqlCmd);

		try {//start SQL statement
			int changed = stmt.executeUpdate(sqlCmd);
			if(changed == 0){//if no updates were made (changed = 0) 
				error = true;//error
				out.println("No user found");//error message
			}
		} catch (SQLException e) {
			error = true;
			out.println(e.getMessage());
			e.printStackTrace();
		}
		if(error == false)
			out.println("true");
	}

	/**
	 * Finds and returns matches based on parameters listed below<br>
	 * 0 - getMatches<br>
	 * 1 - userName<br>
	 * 2 - searching user's longitude<br>
	 * 3 - searching user's latitude<br>
	 * 4 - searching user's pGender min<br>
	 * 5 - searching user's pGender max<br>
	 * 6 - searching user's pExpression min<br>
	 * 7 - searching user's pExpression max<br>
	 * 8 - searching user's pOrientation min<br>
	 * 9 - searching user's pOrientation max<br>
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
		String cLong = commandList.get(2);//holds client's longitude in string form
		String cLat = commandList.get(3);//holds the client's latitude in String form

		ArrayList<String> matchList = new ArrayList<>();//holds user names of found matches
		
		String[] clientPSliders = getPSliders(commandList.get(1));

		//load in SQL command to find matches using given client's min and max values
		command ="SELECT location.userName FROM (location INNER JOIN users "
				+ "ON location.userName = users.userName) INNER JOIN personalSlider "
				+ "ON users.userName = personalSlider.userName WHERE (((location.longitude) <= ";
		command += roundLongUp(cLong) + " And (location.longitude) >= ";
		command += roundLongDown(cLong) + ") AND ((location.latitude) <= ";
		command += roundLatUp(cLat) + " And (location.latitude) >= ";
		command += roundLatDown(cLat) + ") AND ((personalSlider.pGender) >= ";
		command += commandList.get(4) + " And (personalSlider.pGender) <= " + commandList.get(5);
		command += " AND ((personalSlider.pExpression) >= " + commandList.get(6) +
				" And (personalSlider.pExpression) <= " + commandList.get(7) + ")";
		command += " AND ((personalSlider.pOrientation) >= " + commandList.get(8) +
				" And (personalSlider.pOrientation) <= " + commandList.get(9) + ")));";

		
		try {//try block for sending SQL command
			rs = stmt.executeQuery(command);//send command

			while(rs.next()){//while there are matches
				matchList.add(rs.getString("userName"));//load userName
			}

			//each String[] is a match, 0 = userName, 1 = pGenderMin, .....6=pOrientationMax, 7=distance (used later)
			ArrayList<String[]> fullMatches = getSeekingSlider(matchList);

			//remove the non-overlapping matches, pass clients ratings and the list of matches
			crossMatch(strArrToIntArr(clientPSliders), fullMatches);
			
			//remove matches over 25 miles away
			limitDistance(Double.valueOf(cLat), Double.valueOf(cLong), fullMatches);
			
			rs = null;//null the result set
			
			String[] tempStrArr;//holds the personal slider for the current match
			for(String[] currArr: fullMatches){//for all the remaining matches
				tempStrArr = getPSliders(currArr[0]);//get personal slider for current matches
				out.println(tempStrArr[0] + ", " + tempStrArr[1] + ", "//loads userName, pGen, pExpr, pOrient
							+ tempStrArr[2] + ", " + tempStrArr[3] + ", " + currArr[7]);//line 2
			}
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
		String command = "SELECT * FROM seekingSlider WHERE userName = \"";
		ArrayList<String[]> tempList = new ArrayList<>();
		int listSize = inList.size();
		String userName, pGenMin, pGenMax, pExprMin, pExprMax, pOrMin, pOrMax;
		//String[] strArr = new String[7];//holds matches data

		if(listSize == 0) return tempList;//if the list is empty, return empty list

		if(listSize == 1){//if there is only 1 in the list
			command += inList.get(0) + "\";";
		} else {//if there is more than 1
			for(int i = 0; i < listSize; i++){//go through the list
				if(i == 0){//for the first userName
					command += inList.get(i) + "\"";
				} else if(i == listSize-1){//for the last user name
					command += " or userName = \"" + inList.get(i) + "\";";
				} else {//for all other userNames
					command += " or userName = \"" + inList.get(i)+ "\"";
				}
			}//end for loop
		}
		try {//send command and parse results
			rs = stmt.executeQuery(command);

			while(rs.next()){//while there is a result remaining
				userName = rs.getString("userName");
				pGenMin = rs.getString("pGenderMin");
				pGenMax = rs.getString("pGenderMax");
				pExprMin = rs.getString("pExpressionMin");
				pExprMax = rs.getString("pExpressionMax");
				pOrMin = rs.getString("pOrientationMin");
				pOrMax = rs.getString("pOrientationMax");
				//add to the tempList
				tempList.add(new String[]{userName, pGenMin, pGenMax, pExprMin, pExprMax, pOrMin, pOrMax, "-1"});
			}//end while loop
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//out.println(e.getErrorCode());
		}
		return tempList;
	}

	/**
	 * removes matches from the matchList where the client doesn't fit into the matches criteria
	 * @param clientSliders - the personal sliders of the client
	 * @param matchList - the list of matches
	 */
	private void crossMatch(int[] clientSliders, ArrayList<String[]> matchList){
		String[] currArr;//current match String[]
		int[] currentMatch = new int[]{-1,-1,-1,-1,-1,-1,-1};//holds currArr converted into int's

		for(int i = 0; i < matchList.size(); i++){//go through all the matches
			currArr = matchList.get(i);//get current match
			
			for(int j = 1; j < 7; j++){//convert strings to int, 1-6
				currentMatch[j] = Integer.valueOf(currArr[j]);
			}
			
			//check whether the client values are between current match's value
			if(clientSliders[0] < currentMatch[1] || clientSliders[0] > currentMatch[2]//check pGender
					|| clientSliders[1] < currentMatch[3] || clientSliders[1] > currentMatch[4]//check pExpression
					|| clientSliders[2] < currentMatch[5] || clientSliders[2] > currentMatch[6]){//check pOrientation
				matchList.remove(i);//remove match from matchList if values don't fit within
				i--;//compensate for removed match
			}
		}

	}

	/**
	 * sets the user's seeking slider values, these are used to cross match users<br>
	 * 
	 * 0 - setSSlider<br>
	 * 1 - userName<br>
	 * 2 - genderMin<br>
	 * 3 - genderMax<br>
	 * 4 - expressionMin<br>
	 * 5 - expressionMax<br>
	 * 6 - orientationMin<br>
	 * 7 - orientationMax<br>
	 * 
	 * working for proto1<br>
	 */
	private void setSeekingSlider(){
		//INSERT INTO seekingSlider values ('userName', int, int, int, int, int, int);
		String sqlCmd = "INSERT INTO seekingSlider VALUES ('" + commandList.get(1)
		+ "', " + commandList.get(2) + ", " + commandList.get(3)
		+ ", " + commandList.get(4) + ", " + commandList.get(5)
		+ ", " + commandList.get(6) + ", " + commandList.get(7) + ");";
		System.out.println(sqlCmd);
		try {//send the SQL command
			stmt.executeUpdate(sqlCmd);//send command
		} catch (SQLException e) {
			error = true;//set error condition
			System.out.println("sliderError");//write the error code to the client
			out.println(e.getMessage());
			e.printStackTrace();//send  to console
		}
		if (error == false)//if there is no error caught
			out.println("true");//send success to the client
	}


	/**
	 * updates the user's seeking slider that is used to cross match with other users<br>
	 * 
	 * 0 - setSSlider<br>
	 * 1 - userName<br>
	 * 2 - genderMin<br>
	 * 3 - genderMax<br>
	 * 4 - expressionMin<br>
	 * 5 - expressionMax<br>
	 * 6 - orientationMin<br>
	 * 7 - orientationMax<br>
	 * 
	 * working for proto1
	 */
	private void updateSeekingSlider(){
		//UPDATE seekingSlider SET pGenderMin = int, pGenderMax = int, pExpressionMin = int, pExpressionMax = int
		//pOrientationMin = int, pOrientation = int WHERE userName = "userName"
		String sqlCmd = "UPDATE seekingSlider SET pGenderMin = " + commandList.get(2) + ", pGenderMax = " + commandList.get(3)
		+ ", pExpressionMin = " + commandList.get(4) + ", pExpressionMax = " + commandList.get(5)
		+ ", pOrientationMin = " + commandList.get(6) + ", pOrientationMax = " + commandList.get(7)
		+ " WHERE userName = \"" + commandList.get(1) + "\";";
		System.out.println(sqlCmd);
		try {//send the SQL command
			int changed = stmt.executeUpdate(sqlCmd);//send command
			if(changed == 0){
				error = true;
				out.println("User not found, no record updated");
				System.out.println("User not found, no record updated");
			}
		} catch (SQLException e) {
			error = true;//set error condition
			System.out.println("sliderError");//write the error code to the client
			out.println(e.getMessage());
			e.printStackTrace();//send  to console
		}
		if (error == false)//if there is no error caught
			out.println("true");//send success to the client
	}
	
	/**
	 * returns String array of the given user's personal sliders
	 * @param userName - the user whose slider values will be returned
	 * @return - String[], 0 = userName, 1 = pGen, 2 = pExpr, 3 = pOrient
	 */
	private String[] getPSliders(String userName){
		//holds the values
		String pGen = null;
		String pExpr= null;
		String pOrient = null;
		
		//command to get the client's numbers
		String command = "SELECT pGender, pExpression, pOrientation "
				+ "FROM personalSlider WHERE userName = \"" + userName + "\"";
		try {
			rs = stmt.executeQuery(command);//execute statement
			//store numbers
			while(rs.next()){
				pGen = rs.getString("pGender");
				pExpr = rs.getString("pExpression");
				pOrient = rs.getString("pOrientation");
			}
			//write to screen
			//System.out.println(rs.toString());
			rs = null;
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return new String[]{userName, pGen, pExpr, pOrient};//return string array
	}
	
	/**
	 * converts an array of strings into an array of int's
	 * @param strArr - the array to convert
	 * @return an array of integers
	 */
	private int[] strArrToIntArr(String[] strArr){
		//strArr[0] is the userName - skip it, only return pGen, pExpr, pOrientation
		return new int[]{Integer.valueOf(strArr[1]),Integer.valueOf(strArr[2]),Integer.valueOf(strArr[3])};
	}
	
	/**
	 * converts degrees to radians
	 * @param deg double of degrees
	 * @return double of converted radians
	 */
	private static double deg2rad(double deg) {
		return (deg * Math.PI / 180.0);
	}

	/**
	 * 
	 * @param rad
	 * @return
	 */
	private static double rad2deg(double rad) {
		return (rad * 180 / Math.PI);
	}
	
	/**
	 * returns the distance of the given user name from the client's location given as two doubles
	 * @param latitude - Latitude of the client (ie 33.9697)
	 * @param longitude - Longitude of the client (ie -118.2265)
	 * 
	 * @param userName - user to calculate distance to
	 * @return
	 */
	private double distance(Double lat1, Double lon1, String userName){
		//fetch the potential match
		String command = "SELECT latitude, longitude FROM location WHERE userName = \"" + userName + "\";";
		rs = null;//clear previous results
		double lon2 = 0.0;//will hold the match's longitude
		double lat2 = 0.0;//will hold the match's latitude

		try {//try block
			rs = stmt.executeQuery(command);//send command
			while(rs.next()){//parse the results
				lon2 = Double.valueOf(rs.getString("longitude"));
				lat2 = Double.valueOf(rs.getString("latitude"));
			}
		} catch (NumberFormatException | SQLException e) {
			e.printStackTrace();
			out.println("Server error");//server error
		}

		//compute distance
		double theta = lon1 - lon2;
		double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1))
					* Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
		dist = Math.acos(dist);
		dist = rad2deg(dist);
		dist = dist * 60 * 1.1515;
		rs = null;
		
		return dist;//the distance computed
	}
	
	/**
	 * removes any matches that are further than the max distance
	 * @param latitude - Latitude of the client's location
	 * @param longitude - Longitude of the client's location
	 * @param arrList - the match list
	 */
	private void limitDistance(double latitude, double longitude, ArrayList<String[]> arrList){
		String[] currArr;//will hold match that is being checked
		Double dist = 0.0;//holds the computed distance
		
		//for the entire match list
		for(int i = 0; i < arrList.size(); i++){
			currArr = arrList.get(i);//current match
			dist = distance(latitude, longitude, currArr[0]);//compute the distance
			
			if(dist > MAX_DISTANCE){//check for distance
				arrList.remove(i);//remove violating match
				i--;//compensate for removed item
			} else {
				currArr[7] = String.valueOf(dist);//add the distance from the client
			}
		}
	}
	
	/**
	 * test to see if the given username already exists
	 */
	private void testUserName(){
		//get user with the 'test' userName
		String command = "SELECT userName FROM users "
				+ "WHERE userName = \"" + commandList.get(1) + "\";";
		System.out.println(command);//print command
		try {//send command
			rs = stmt.executeQuery(command);
			
			if(rs.next()){//if it returns user, userName exists
				out.println("exists");
			} else {//no results returned, empty
				out.println("free");
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			out.println(e.getMessage());//send back error message
		}
		

	}

}//end class