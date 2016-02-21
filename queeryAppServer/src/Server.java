

import java.net.ServerSocket;
import java.net.Socket;

public class Server {
	private static final int SERVERPORT = 4910;

	//skip constructor 
	//creates AppServer after Waiting for connection
	public static void main(String[] args) {
		Boolean serverStatus = true;
		
		while(serverStatus){
	
			try{
				ServerSocket server = new ServerSocket(SERVERPORT);//makes a new socket.	
				
				System.out.println("Waiting...");//until connection
				Socket sock = server.accept();//wait for a connection
				
				AppServer s = new AppServer(sock);//make AppServer object to handle connection
				System.out.println("Connected: ");//connection complete
				
				serverStatus = s.getServerStatus();//defaults to true, change on kill command
				
				if(serverStatus == false){
					System.out.println("ending the server");
				}
				
				//end connection
				System.out.println("Goobye");
				
				//for debugging
				serverStatus = false;
				
				
			} catch(Exception e){
				System.out.println(e);
			}
		}
	}

}
