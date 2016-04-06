import java.net.ServerSocket;
import java.net.Socket;

public class DataServer{
	ServerSocket server = null;//empty ServerSocket
	public static Boolean serverStatus = true;
	int SERVERPORT = 6066;
	int connNum = 0;

	public DataServer(){
		try{//start
			server = new ServerSocket(SERVERPORT);//makes a new socket.	

			while(serverStatus){
				System.out.println("Pic Server Waiting...");//until connection
				Socket sock = server.accept();//wait for a connection
				connNum++;
				System.out.println("Pic Server connection #" + connNum + " to " + sock.getRemoteSocketAddress());


				new Thread(new PicServer(sock, serverStatus)).start();//make AppServer object to handle connection
			}
			System.out.println("ending pic server");
		} catch(Exception e){//something went wrong
			System.out.println(e);
		} finally {//close out on end
			try {
				if(server != null){
					server.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.out.println("Kill signal received, goodbye!");
	}


}
