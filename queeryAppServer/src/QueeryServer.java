

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Calendar;


public class QueeryServer {
	private static final int SERVERPORT = 4910;
	public static Boolean serverStatus = true;
	long connNum = 0;
	
	public QueeryServer(){//build the thread factory
		ServerSocket server = null;//empty ServerSocket

		try{//start
			server = new ServerSocket(SERVERPORT);//makes a new socket.	
			while(serverStatus){
				System.out.println("Queery Server Waiting...");//until connection
				Socket sock = server.accept();//wait for a connection
				connNum++;
				System.out.println("Connected: #" + connNum + Calendar.getInstance().getTime());//connection complete
				
				
				new Thread(new AppServer(sock, serverStatus)).start();//make AppServer object to handle connection
			}
			System.out.println("ending queery server");
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
