import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.sql.*;
import javax.imageio.ImageIO;
import org.imgscalr.Scalr;

public class PicServer implements Runnable {
	private Socket sock;
	private static final String DB_URL = "jdbc:mysql://cecs-db01.coe.csulb.edu/"
			+ "cecs491bp?autoReconnect=true&useSSL=false";
	private static final String USER = "cecs491a11";
	private static final String PASS = "ohChox";//hard coded password
	private Connection conn = null;//empty JDBC sql connection
	private BufferedImage bimg;//will hold the image, coming or going
	private DataInputStream din;//reads data in
	private DataOutputStream dout;//writes data out
	private String userName;//userName of the affected party
	private String command;//the command sent to the sever
	private String password;//the user's password
	private ByteArrayOutputStream baos;//stream to convert image to usable form
	private InputStream is;//input stream used for writing BLOB
	private PreparedStatement pre;//prepared statement to send to SQL server

	/**
	 * Default constructor that builds the PicServer
	 * @param sock - connection passed from the DataServer class
	 * @param serverStatus - used to kill the server
	 */
	public PicServer(Socket sock, boolean serverStatus, String[] args) {
		this.sock = sock;//copy the connection
		try {
			//instantiate the input stream
			din = new DataInputStream(sock.getInputStream());
			//instantiate the output stream
			dout = new DataOutputStream(sock.getOutputStream());
			Class.forName("com.mysql.jdbc.Driver");//load JDBC driver
			conn = DriverManager.getConnection(DB_URL,USER,PASS);// connection
			//conn = DriverManager.getConnection(args[0],args[1],args[2]);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();//for the DataStreams, can't write back error
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			try {//write back to the client
				dout.writeUTF(e.getMessage());
			} catch (IOException e1) {
				e1.printStackTrace();//error on writing back to the client
			}
		} catch (SQLException e) {//error on the connection
			e.printStackTrace();
			try {//write back error to the client
				dout.writeUTF(e.getMessage());
			} catch (IOException e1) {
				// can't write back to the client
				e1.printStackTrace();
			}
		}
	}

	/**
	 * run the thread
	 */
	public void run(){
		try {//read in the command and parse
			String[] params = din.readUTF().split("\\s*,\\s*");
			command = params[0];//store the command
			userName = params[1];//store userName

			switch(command){//switch on the addPic, updatePic, getPic

			case "addPic"://for new users
				password = params[2];//store the password
				if(checkPassword(userName, password)){//check for valid password
					dout.writeUTF("true");//on password success
					//call addPic function to add profile pic
					addPic(command, userName, password);
				} else {//send error to the client
					dout.writeUTF("password/user failure");
				}
				break;

			case "updatePic"://for existing users
				password = params[2];//store the password
				if(checkPassword(userName, password)){//check for valid password
					dout.writeUTF("true");//on password success
					addPic(command, userName, password);//call addPic to update
				} else {
					dout.writeUTF("Password failure");//write back password error
				}
				break;

			case "getPic"://to get the profile pic of a certain user
				getPic(userName);
				break;
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			try {
				dout.writeUTF(e.getMessage());
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				System.out.println("couldn't send error message");
				e1.printStackTrace();
			}//end inner catch
		} finally {//cleanup
			try {
				din.close();
				dout.close();
				conn.close();
				sock.close();
			} catch (IOException | SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}//end run


	/**
	 * called when adding or updating the profile pic of a user<br>
	 * <br>
	 * bwriteUTF("command, userName, password"); used to call from client<br>
	 * command should be either addPic for new users or updatePic 
	 * for existing users.<br>
	 * 
	 * @param command addPic or updatePic
	 * @param userName - username to be updated
	 * @param password - password of the user name
	 */
	public void addPic(String command, String userName, String password){
		try{
			System.out.println("Adding Picture for " + userName);
			
			//read the image into a buffered reader 
			bimg = ImageIO.read(ImageIO.createImageInputStream(sock.getInputStream()));
			
			//if the read failed, the buffered image will be null
			if(bimg == null){
				dout.writeUTF("Failed to receive image");
			}
			
			//this will hold the image stream
			baos = new ByteArrayOutputStream();
			
			//resize if either dimensions is larger than 1080
			if(bimg.getHeight() > 1080 || bimg.getWidth() > 1080){
				System.out.println("Rescaling image from " + bimg.getWidth() 
									+ " x " +bimg.getHeight() );
				
				bimg = Scalr.resize(bimg, 1080);
				System.out.println("to " + bimg.getWidth() 
									+ " x " +bimg.getHeight());
			}
			//check that resize worked
			if(bimg == null){
				dout.writeUTF("Failed to resize image");
			}

			//write image to byte array
			ImageIO.write(bimg, "jpg", baos);
			
			//the write ByteArray into an input stream to load into SQL
			is = new ByteArrayInputStream(baos.toByteArray());
			
			//check if SQL works.
			if(is != null){
				if(command.equals("addPic")){//add picture command
					pre = conn.prepareStatement("insert into images "
									+ "(userName, profilePic) values (?,?)");
					
					pre.setString(1,userName);
					pre.setBinaryStream(2, is, (int)baos.size());
				} else {//update picture command
					pre = conn.prepareStatement("update images set profilePic "
													+ "= ? where userName = ?");
					pre.setBinaryStream(1, is, (int)baos.size());
					pre.setString(2, userName);
				}

				//did the insert work?
				if(pre.executeUpdate() == 1){
					dout.writeUTF("true");
				} else {//insert fail
					dout.writeUTF("Adding to SQL failed");
				}
			} else {
				dout.writeUTF("Image fail");
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			try {
				dout.writeUTF(e.getMessage());
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				System.out.println("Can't send error message");
				e1.printStackTrace();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Can't send error message");
			e.printStackTrace();
		} finally {
			try {
				pre.close();
				conn.close();
				if(is != null)
					is.close();
			} catch (SQLException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}//end addPic

	/**
	 * returns a picture to the computer who is requesting the picture of the
	 * user.
	 * 
	 * @param userName the user who profile pic is requested
	 */
	public void getPic(String userName){
		try{
			String query = "SELECT profilePic FROM images WHERE userName = ?;";
			pre = conn.prepareStatement(query);//make the query statement
			pre.setString(1, userName);//insert user name
			ResultSet rs = pre.executeQuery();//execute query
			
			if(rs.next()){//for every returned result
				Blob blob = rs.getBlob("profilePic");//load in blob
				//blob to byte array
				byte[] rsBlob = blob.getBytes(1, (int)blob.length());
				dout.writeUTF(String.valueOf(rsBlob.length));//send size
				dout.flush();//needed?
				dout.write(rsBlob);//send byte array
				dout.close();//close out
				sock.close();//close socket
				System.out.println("Returning the pic for " + userName);
				System.out.println("Image sent to " + sock.getRemoteSocketAddress());
			} else {
				dout.writeUTF("0");
			}
		}catch (IOException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			try {
				dout.writeUTF("0");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} finally {
			try {
				if(sock != null)
					sock.close();
				pre.close();
				conn.close();
			} catch (SQLException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Checks the users table to verify that the password matches the username
	 * @param un - username to check
	 * @param pass - password to verify
	 * @return boolean of match condition, uses .equals(string) 
	 */
	private boolean checkPassword(String un, String pass){
		String query = "SELECT password FROM users WHERE userName = '" + un + "';";
		try {
			pre = conn.prepareStatement(query);
			ResultSet rs = pre.executeQuery();
			rs.next();//advance
			String returnedPass = rs.getString("password");//get pass
			return returnedPass.equals(pass);//return comparison

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;//default to false
	}
}
