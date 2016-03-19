

import java.net.ServerSocket;
import java.net.Socket;


public class QueeryServer {
	private static final int SERVERPORT = 4910;
	public static Boolean serverStatus = true;

	//skip constructor 
	//creates AppServer after Waiting for connection
	public static void main(String[] args) {
		new QueeryServer();//run thread factory
	}
	
	public QueeryServer(){//build the thread factory
		ServerSocket server = null;//empty ServerSocket

		try{//start
			server = new ServerSocket(SERVERPORT);//makes a new socket.	
			while(serverStatus){
				System.out.println("Waiting...");//until connection
				Socket sock = server.accept();//wait for a connection
				System.out.println("Connected: ");//connection complete
				
				new Thread(new AppServer(sock, serverStatus)).start();//make AppServer object to handle connection

				//serverStatus = s.getServerStatus();//defaults to true, change on kill command

				if(serverStatus == false)//quit on serverKill
					System.out.println("ending the server");
			}
		} catch(Exception e){//something went wrong
			System.out.println(e);
		} finally {//close out on end
			try {
				server.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.out.println("Kill signal received, goodbye!");
	}

}
