/**
 * 
 */
package ngat.sms.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;

import ngat.net.TelnetSocketServer;

/**
 * @author eng
 *
 */
public class FakeAutoguiderTempServer {

	static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-Mm-dd'T'HH:mm:ss.SSS");
	static SimpleTimeZone UTC = new SimpleTimeZone(0, "UTC");
	
	int port;
	
	double agt = -40.0;
	
	ServerSocket server;


	/**
	 * @param port
	 */
	public FakeAutoguiderTempServer(int port) throws Exception {	
		this.port = port;
		server = new ServerSocket(port);
		System.err.printf("FATS:: Server bound: %6d\n", port);
	}

	public void start() {
		
		while (true) {
			
			try {
				System.err.println("FATS:: Server listening...");
				Socket socket = server.accept();
				
				Handler h = new Handler(socket);
				h.start();
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		try {
		
			int port = Integer.parseInt(args[0]);
			FakeAutoguiderTempServer fats = new FakeAutoguiderTempServer(port);
			fats.start();

		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	private class Handler extends Thread {
		
		Socket mySocket;
		
		Handler(Socket mySocket) {
			this.mySocket = mySocket;
		}
		
		public void run() {
			
			try {
					
				BufferedReader bin = new BufferedReader(new InputStreamReader(mySocket.getInputStream()));
				String request = bin.readLine();
				System.err.println("FATS.Socket::Received: ["+request+"]");
				
				agt += (Math.random() - 0.5)*10.0;
				if (agt < -50.0)
					agt += 1.0;
				if (agt > -20.0)
					agt -= 1.0;
				
				// assume [status temperature get]
				
				PrintStream pout = new PrintStream(mySocket.getOutputStream());
				
				// reply e.g. 0  2010-10-08T15:34:33.004 -34.9  
				pout.printf("0 %s %4.2f \n", sdf.format(new Date()), agt);
				System.err.printf("FATS.Socket::Replying with [0 %s %4.2f] \n", sdf.format(new Date()), agt);
				pout.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		
	}
	
}
