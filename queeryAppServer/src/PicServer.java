import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;

public class PicServer implements Runnable {
	private ServerSocket srvSock;
	private Socket sock;
	private static final String DB_URL = "jdbc:mysql://cecs-db01.coe.csulb.edu/cecs491bp?autoReconnect=true&useSSL=false";
	private static final String USER = "cecs491a11";
	private static final String PASS = "ohChox";//hard coded password
	private Connection conn = null;
	private BufferedImage bimg;
	private DataInputStream din;
	private DataOutputStream dout;
	private String userName;
	private String command;
	private String password;
	private ByteArrayOutputStream baos;
	private InputStream is;
	private PreparedStatement pre;

	public PicServer(Socket sock, boolean serverStatus) {
		this.sock = sock;
		try {
			din = new DataInputStream(sock.getInputStream());
			dout = new DataOutputStream(sock.getOutputStream());
			Class.forName("com.mysql.jdbc.Driver");//load JDBC driver
			conn = DriverManager.getConnection(DB_URL,USER,PASS);//complete connection
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}

	public void run(){
			try {

				//baos = new ByteArrayOutputStream();
				//ImageIO.write(bimg, "JPG", baos);
				//is = new ByteArrayInputStream(baos.toByteArray());
				String[] params = din.readUTF().split("\\s*,\\s*");
				command = params[0];
				userName = params[1];

				switch(command){

				case "addPic":
					password = params[2];
					if(checkPassword(userName, password)){
						dout.writeUTF("true");
						addPic(command, userName, password);
					} else {
						dout.writeUTF("Password failure");
					}
					break;

				case "updatePic":
					password = params[2];
					if(checkPassword(userName, password)){
						dout.writeUTF("true");
						addPic(command, userName, password);
					} else {
						dout.writeUTF("Password failure");
					}
					break;

				case "getPic":
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
			bimg = ImageIO.read(ImageIO.createImageInputStream(sock.getInputStream()));
			ImageIO.write(bimg, "JPG", baos);
			is = new ByteArrayInputStream(baos.toByteArray());
			
			if(command == "addPic"){
				pre = conn.prepareStatement("insert into images (userName, profilePic) values (?,?)");
				pre.setString(1,userName);
				pre.setBinaryStream(2, is, (int)baos.size());
			} else {
				PreparedStatement pre = conn.prepareStatement("UPDATE images SET profilePic = ? WHERE userName = ?");
				pre.setBinaryStream(1, is, (int)baos.size());
				pre.setString(2, userName);
			}

			if(pre.executeUpdate() == 0){
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
			return rs.getString("password") == pass;

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}



		return false;
	}

}
