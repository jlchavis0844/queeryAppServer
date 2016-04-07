import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.sql.*;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;

public class PicServer implements Runnable {
	private Socket sock;
	private static final String DB_URL = "jdbc:mysql://cecs-db01.coe.csulb.edu/cecs491bp?autoReconnect=true&useSSL=false";
	private static final String USER = "cecs491a11";
	private static final String PASS = "ohChox";//hard coded password
	private Connection conn = null;//empty JDBC sql connection
	private BufferedImage bimg;//will hold the image, comming or going
	private DataInputStream din;//reads data in
	private DataOutputStream dout;//writes data out
	private String userName;//userName of the affected party
	private String command;//the command sent to the sever, addPic, updatePic, getPic
	private String password;//the user's password
	private ByteArrayOutputStream baos;//stream to convert image to writable formats
	private InputStream is;//input stream used for writing BLOB
	private PreparedStatement pre;//prepared statement to send to SQL server

	/**
	 * Default constructor that builds the PicServer
	 * @param sock - connection passed from the DataServer class
	 * @param serverStatus - used to kill the server
	 */
	public PicServer(Socket sock, boolean serverStatus) {
		this.sock = sock;//copy the connection

		try {
			din = new DataInputStream(sock.getInputStream());//instantiate the input stream
			dout = new DataOutputStream(sock.getOutputStream());//instantiate the output stream
			Class.forName("com.mysql.jdbc.Driver");//load JDBC driver
			conn = DriverManager.getConnection(DB_URL,USER,PASS);//complete connection
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();//for the DataStreams, can't write back the error message to client
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
		try {
			String[] params = din.readUTF().split("\\s*,\\s*");//read in the command and parse
			command = params[0];//store the command
			userName = params[1];//store userName

			switch(command){//switch on the addPic, updatePic, getPic

			case "addPic"://for new users
				password = params[2];//store the password
				if(checkPassword(userName, password)){//check for valid password
					dout.writeUTF("true");//on password success
					addPic(command, userName, password);//call addPic function to add profile pic
				} else {//send error to the client
					dout.writeUTF("Password failure");
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
				pre.close();
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
	 * command should be either addPic for new users or updatePic for existing users.<br>
	 * 
	 * @param command 
	 * @param userName
	 * @param password
	 */
	public void addPic(String command, String userName, String password){
		try{
			//bimg = ImageIO.read(ImageIO.createImageInputStream(sock.getInputStream()));//read in the image
			bimg = ImageIO.read(din);
            JDialog dialog = new JDialog();
            //dialog.setUndecorated(true);
            JLabel label = new JLabel(new ImageIcon(bimg));
            dialog.add(label);
            dialog.pack();
            dialog.setVisible(true);
            baos = new ByteArrayOutputStream();
			ImageIO.write(bimg, "JPG", baos);//write to ByteArrayOutputStream
			is = new ByteArrayInputStream(baos.toByteArray());//convert to inputstream
			
			if(command.equals("addPic")){
				pre = conn.prepareStatement("insert into images (userName, profilePic) values (?,?)");
				pre.setString(1,userName);
				pre.setBinaryStream(2, is, (int)baos.size());
			} else {
				pre = conn.prepareStatement("update images set profilePic = ? where userName = ?");
				pre.setBinaryStream(1, is, (int)baos.size());
				pre.setString(2, userName);
			}

			if(pre.executeUpdate() == 1){
				dout.writeUTF("true");
			} else {
				dout.writeUTF("Adding to SQL failed");
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

	public void getPic(String userName){
		try{
			String query = "SELECT profilePic FROM images WHERE userName = ?;";
			pre = conn.prepareStatement(query);
			pre.setString(1, userName);
			ResultSet rs = pre.executeQuery();

			if(rs.next()){
				dout.writeUTF("true");
				dout.flush();
				Blob blob = rs.getBlob("profilePic");
				byte[] rsBlob = blob.getBytes(1, (int)blob.length());
				is = new ByteArrayInputStream(rsBlob);
				bimg = ImageIO.read(is);

				/* JDialog dialog = new JDialog();
	            //dialog.setUndecorated(true);
	            JLabel label = new JLabel(new ImageIcon(bimg));
	            dialog.add(label);
	            dialog.pack();
	            dialog.setVisible(true);*/

				//is.close();
				ImageIO.write(bimg, "JPG",sock.getOutputStream());
				sock.getOutputStream().close();
				System.out.println("Image sent to " + sock.getRemoteSocketAddress());
			} else {
				dout.writeUTF("User not found");
			}


			//File output = new File("");
			//ImageIO.write(bimg, "jpg", output);


		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				pre.close();
				conn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private boolean checkPassword(String un, String pass){
		String query = "SELECT password FROM users WHERE userName = '" + un + "';";
		try {
			pre = conn.prepareStatement(query);
			ResultSet rs = pre.executeQuery();
			rs.next();
			String returnedPass = rs.getString("password");
			return returnedPass.equals(pass);
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}



		return false;
	}

}
