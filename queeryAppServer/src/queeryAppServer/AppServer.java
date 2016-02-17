package queeryAppServer;

import java.net.Socket;

public class AppServer implements Runnable{

	public static final int PORT = 491;
	Socket csocket;
	
	AppServer(Socket csocket){
		this.csocket = csocket;
	}
	public static void main(String[] args) {

		
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

}
