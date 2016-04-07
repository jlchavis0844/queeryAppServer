import java.util.Calendar;

public class RunMe {
	//creates AppServer after Waiting for connection
	public static void main(String[] args) {
		
		
		System.out.println("Server Started at " + Calendar.getInstance().getTime());
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
