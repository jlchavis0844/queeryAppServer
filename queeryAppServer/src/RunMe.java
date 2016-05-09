import java.util.Calendar;

/**
 * Starts the multiple server types
 * @author James
 *
 */
public class RunMe {
	//creates AppServer after Waiting for connection
	public static void main(String[] args) {
		
		/*if (args.length != 3){
			System.out.println("On'y " + args.length +" args, give URL, user, password");
			System.exit(-1);
		}*/
		
		System.out.println("Server Started at " + Calendar.getInstance().getTime());
		//start the AppServer for user info
		new Thread(){
			public void run(){
				new QueeryServer(args);//run thread factory
			}
		}.start();
		
		//start the DataServer for user pictures
		new Thread(){
			public void run(){
				new DataServer(args);//run thread factory
			}
		}.start();
		
		//add messaging server here.
	}

}
