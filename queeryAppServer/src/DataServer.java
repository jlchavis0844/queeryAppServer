import java.net.ServerSocket;
import java.net.Socket;
import java.util.Calendar;

/**
 * This class will start and maintain several PicServer threads. There
 * are not params or returns
 * @author James
 *
 */
public class DataServer{
	ServerSocket server = null;//empty ServerSocket
	public static Boolean serverStatus = true;//used to kill the server
	int SERVERPORT = 6066;//port to accept connections on
	int connNum = 0;//keeps track of how many connections have been made

	/**
	 * Default constructors that creates PicServer threads
	 */
	public DataServer(){
		try{//start
			server = new ServerSocket(SERVERPORT);//makes a new socket.	

			while(serverStatus){//while still going
				System.out.println("Pic Server Waiting...");//until connection
				Socket sock = server.accept();//wait for a connection
				connNum++;//increase connection count
				System.out.println("Pic Server connection #" + connNum + " at " + "Server Started at " + Calendar.getInstance().getTime());


				new Thread(new PicServer(sock, serverStatus)).start();//make AppServer object to handle connection
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
