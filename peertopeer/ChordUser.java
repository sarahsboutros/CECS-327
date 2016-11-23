import java.rmi.*;
import java.net.*;
import java.util.*;
import java.io.*;;
import java.rmi.MarshalException;
import java.io.FileInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

 
public class ChordUser
{
    
     int port;
     public ChordUser(int p) {
        port = p;
        
	Timer timer1 = new Timer();
	timer1.scheduleAtFixedRate(new TimerTask() {
		/// Begins the Process of joining writting reading deleting printing and leaving
	    @Override
	    public void run() {
	      try {
	      
		Chord    chord = new Chord(port);
		    
		Scanner scan= new Scanner(System.in);
		String delims = "[ ]+";
		String command = "";
		while (true)
		{
			System.out.println("Usage: \n\tjoin <port>\n\twrite <file> (the file must be an integer stored in the working directory, i.e, ./port/file");
		System.out.println("\tread <file>\n\tdelete <file>\n\tprint <file>\n\tleave");
        
    
		  String text= scan.nextLine();
		  String[] tokens = text.split(delims);
		    if (tokens[0].equals("join") && tokens.length == 2) {
			try {
			  chord.joinRing("localhost", Integer.parseInt(tokens[1]));
			} catch (IOException e) {
			      System.out.println("Error joining the ring!");
			}
		    }
		    if (tokens[0].equals("print")) {
			chord.Print();
		    }
		    if  (tokens[0].equals("write") && tokens.length == 2) {
			
			try {	
				String path;
				int guid = Integer.parseInt(tokens[1]);
				// If you are using windows you have to use
// 				path = ".\\"+  port +"\\"+Integer.parseInt(tokens[1]); // path to file
				path = "./"+  port +"/" + "/repository/" +guid; // path to file
				FileStream file = new FileStream(path);
				ChordMessageInterface peer = chord.locateSuccessor(guid);
				peer.put(guid, file); // put file into ring
			} catch (FileNotFoundException e1) {
				//e1.printStackTrace();
				System.out.println("File was not found!");
			} catch (RemoteException e1) {
				e1.printStackTrace();
				System.out.println("File was not found!");
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Could not put file!");
			}
		    }
		    if  (tokens[0].equals("read") && tokens.length == 2) {
                        try {
                            // TODO
                            int guid = Integer.parseInt(tokens[1]);
                            InputStream i = chord.get(guid);
                            FileOutputStream f = new FileOutputStream("./" + port + "/" + "/repository/" + guid + "");
                            while(i.available() > 0){
                                f.write(i.read());
								System.out.println(i.read());
                            }
                            i.close();
                            f.flush();
                            f.close();
                            
 			} catch (IOException e) {
 			      System.out.println("Could not get file!");
 			}
		    }
		    if  (tokens[0].equals("delete") && tokens.length == 2) {
			try {
			  chord.delete(Integer.parseInt(tokens[1]));
			} catch (IOException e) {
			      System.out.println("Could not delete file!");
			}
		    }
                    if (tokens[0].equals("leave"))
			{
                            System.out.println("Leaving...");
                            chord.leaveRing();
                            System.exit(1);
			}
		  }
		}
		catch(RemoteException e)
		{
		}		
	      }
	   }, 1000, 1000);
    }
    
    static public void main(String args[])
    {
	if (args.length < 1 ) {  
	  throw new IllegalArgumentException("Parameter: <port>");
      }
        try{
	  ChordUser chordUser=new ChordUser( Integer.parseInt(args[0]));
	}
	catch (Exception e) {
           e.printStackTrace();
           System.exit(1);
	}
     } 
}