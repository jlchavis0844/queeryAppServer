import java.util.Calendar;

public class RunMe {
	//creates AppServer after Waiting for connection
	public static void main(String[] args) {
		
		/*if (args.length != 3){
			System.out.println("On'y " + args.length +" args, give URL, user, password");
			System.exit(-1);
		}*/
		
		System.out.println("Server Started at " + Calendar.getInstance().getTime());
		new Thread(){
			public void run(){
				new QueeryServer(args);//run thread factory
			}
		}.start();
		
		new Thread(){
			public void run(){
				new DataServer(args);//run thread factory
			}
		}.start();
	}

}
