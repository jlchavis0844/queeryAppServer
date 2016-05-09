import java.net.ServerSocket;
import java.net.Socket;
import java.util.Calendar;

/**
 * This class will start and maintain several PicServer threads. 
 * @author James
 *
 */
public class DataServer{
	ServerSocket server = null;//empty ServerSocket
	public static Boolean serverStatus = true;//used to kill the server
	int SERVERPORT = 6066;//port to accept connections on
	int connNum = 0;//keeps track of how many connections have been made

	/**
	 * Constructor
	 * @param args - Params of server address, DBusername, DBpassword
	 */
	public DataServer(String[] args){
		try{//start
			server = new ServerSocket(SERVERPORT);//makes a new socket.	

			while(serverStatus){//while still going
				System.out.println("Pic Server Waiting...");//until connection
				Socket sock = server.accept();//wait for a connection
				connNum++;//increase connection count
				System.out.println("Pic Server connection #"+connNum+" at " + 
				sock.getRemoteSocketAddress() +	" Server Started at "
						+ Calendar.getInstance().getTime());
				//make AppServer object to handle connection
				new Thread(new PicServer(sock, serverStatus, args)).start();
			}
		} catch(Exception e){//something went wrong
			System.out.println(e);
		} finally {//close out on end
			try {
				if(server != null){//shutdown the socket
					server.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.out.println("Kill signal received, goodbye!");
	}
}
