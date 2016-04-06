

public class RunMe {
	//creates AppServer after Waiting for connection
	public static void main(String[] args) {
		new Thread(){
			public void run(){
				new QueeryServer();//run thread factory
			}
		}.start();
		
		new Thread(){
			public void run(){
				new DataServer();//run thread factory
			}
		}.start();
	}

}
