Queery Application Server
****************************************************************
The Queery Application Server is meant to be the server portion of the Queery dating application. This server is meant to be the middle-man between the android client and the SQL database. The architecture of this project is a simple Client-Server design. The Client will have its own README/developers guide. The premise of the Queery application is to “… create a mobile dating experience that meets the unique needs of people with fluid preferences for romantic and sexual partners. This includes people in the queer and LGBT communities, as well as straight-identified people whose preferences are not represented by the choices on popular dating sites.” Queery is built around using three sliders to represent the user’s sex, gender expression, and sexual orientation. These three sliders are called “personal sliders”. The user then defines acceptable ranges based on sex, gender expression, and sexual orientation. These ranges are called “seeking sliders”. In order for a match between two users to be found, both users’ personal sliders must fall within each-other users seeking slider and be within 25 miles. Only both users’ personal sliders fall within each other’s seeking sliders does a match occur. In order to accomplish this, the client registers a new user with the standard information such as username, e-mail, password, date of birth, first name, and last name. Then the user picks their three personal slider values and their six seeking slider values (a min and max value for each of the three categories).  Once the client gathers the information, it then send the information to the application server. The application server then stores the data into the appropriate tables in the SQL database. The server performs several set and get functions with the information inside the SQL database. The server receives the request from the database, fetches the appropriate information from the SQL database, manipulates the data, and then returns the data to the client. Below is a sample command received from the client:

  addUser, User491, pass491, User, Fournineone, user@491.net, 12/31/1990

The client will then return “true” if successfully added or an error message such as “Duplicate entry 'User491' for key 'PRIMARY'…” if the user was not added. For images, the client sends the get/add/update command, the client then sends the image, the server receives the image, and then sends back replay.

FEATURES:
*****************************************************************
-	Add a new user
-	Login command to check user name and password
-	Test if a user name exists already
-	Set and/or update an users personal slider values
-	Set and/or update an users seeking slider values
-	Set the users location which is used in determining distance
-	Matches users who meet the user’s criteria and is with 25 miles.
-	Add or update a users’ profile picture
-	Get a user’s picture

INSTALLATION:
*******************************************************************
In order to install the server the user can either download the source code directly, download the JAR file or download the EXE file. The user simply needs to compile the source code at which point the RunMe.class can be launched from the java command line simply using “java RunMe params”. The server will run until it is killed locally. Running the server also requires that it be launched using 3 parameters: the URL to the SQL database, the user for the SQL database login, and the password to the database. In order to compile and run the Queery Application Server, you must meet the following minimum requirements:

- Java 8 JRE (1.8.0) installed.
- A supported compiler.
- MySQL JDBC driver placed in the …/jre 1.8.0_*/lib/ext/ folder.
- Any OS that supports java (Mac OS, Windows, Linux, etc).
    
Alternately, you may also download the precompiled JAR file. You can launch the server by running the JAR file but this will launch a headless process. To run with a console view, run the associated BAT file. All external libraries are included in the JAR file. You simply need Java 8 and an OS capable of running Java applications.

Also, you may also download the precompiled EXE file. You can launch the server by running the EXE file but this will launch a headless process. To run with a console view, run the associated BAT file. All external libraries are included in the EXE file. You simply need Java 8 and an OS capable of running Java applications.

Both methods require the server to be run with parameters. 

COMPONENTS:
*********************************************************************
The Queery Application Server is broken into two threaded servers. One server is used for the processing of data in the form of basic strings. The other server is built to handle images. Each server is threaded, meaning that once the server socket connection is accepted from the client, a thread is created, given the connection, and allowed to continue the operation while a new socket is created. The new socket then waits for the next connection. This enables multiple connections at the same time and ensures that connections are not reject. At this time, an Executor or thread pool is not used to limit the amount of connections. These two servers are both created by the RunMe class that contains the main function.  RunMe simply builds both the text server and the data server and starts them. This allows for the Queery server to add new features by simply adding another type of server. For Instance, to add a messaging server, a new class would simply need to be instantiated by the RunMe class.  It is possible for both servers to connect to either a local or remote SQL database. The IP address of 127.0.0.1 should be used to connect to a local server. Both servers work on the premise that a command string is sent as a single line of comma separated values. The string is parsed into a List of substrings which act as parameters.

The text server (AppServer) is a threaded class that has two major pieces to it: the socket and the database connection. The socket’s input stream is opened with an InputStreamReader wrapped in a BufferedReader. This is good for String objects but not data. The output stream simply uses a PrintStream object.  The socket is Java’s net.Socket library and the readers and writers are Java’s IO library. Since all data is sent and received as Strings, no special considerations or libraries are needed for the socket and its streams. For the SQL connection, the AppServer uses MySQL’s JDBC driver version 5.1.38 driver available here (https://dev.mysql.com/downloads/connector/j/). For the sake of simplicity, the entire java.sql.* library is imported.  The Connection, ResultSet, and Statement classes all belong to the JDBC driver loaded through the java.sql.* import. No other special classes are needed to complete the SQL connection. If compiling from source code, you may wish to hard code the values for your SQL URL and even your user name because these are unlikely to change. The password should remain parameterized as it should be changed routinely, even on a local server. 
	
The image server (PicServer) in that it uses DataInputStream and DataOutputStream to write data to and from the client whereas the AppServer used standard Input Output streams. The general structure of the PicServer remains the same as the AppServer with the exception of the input and output streams. The for Strings, the change is trivial {out.println() vs out.writeUTF()}. Writing an image to and from the client much more effort. Android does not support Java’s standard ImageIO commands from its java.awt.* libraries. To write a picture to the client, the profile picture is retrieved from the SQL server in the form of a Blob. The Blob is then loaded into a byte array.  The length of this byte array is then written to the client so it knows how many bytes to read. The blob is then byte array is then written to the output stream. To read an image from the client is easier because the image can be read using java.awt.* using ImageIO’s createImageInputStream() wrapped in ImageIO.read(). Once the image is read in, it is placed in a Buffered image. Because using large images places a heavy burden on the server, the image is resized to a max length or height of 1080 pixels using the open-source library imgscalr available here (https://github.com/thebuzzmedia/imgscalr). The image must then be converted into a ByteArrayInputStream to be placed into the SQL database for the given user. If no error is detected during the read, resize, or SQL insert, true is written back. An error message is sent if something fails.
	
The SQL database is a MySQL database, see here (https://www.mysql.com/) for more info. This server was developed and tested using a database host by the Computer Engineering and Computer Science department at California State University Long Beach. Any MySQL server should work. MySQL is open-source under the GNU license. The JDBC driver must match the type of SQL database.  The database contains the following tables :

- Users – holds the username, password, email, first name, last name, and date of birth
- Location – holds the username, longitude, latitude
- personalSlider – username, sex, gender expression, orientation
- seekingSlider – username, sex minimum, sex maximum, expression minimum, expression maximum, orientation minimum, orientation maximum
- images – username, profile picture

CONTRIBUTE
*************************************************************
Source code - https://github.com/jlchavis0844/queeryAppServer
Issue tracker - https://github.com/jlchavis0844/queeryAppServer/issues
There are currently only two branches, a master and the current development branch. All non-contributors should clone to local.

SUPPORT
**************************************************************
Follow our application using on the web at http://www.queerydating.com/
You can also follow the application using our social media accounts: Facebook, Twitter, or Instagram.
Technical questions and/or problems can be directed to the accounts listed above or submitted to the issue tracker.
