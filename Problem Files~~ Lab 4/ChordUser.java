import java.rmi.*;
import java.net.*;
import java.util.*;
import java.io.*;;

 
public class ChordUser
{
     int port;
     public ChordUser(int p) {
        port = p;
        
	Timer timer1 = new Timer();
	timer1.scheduleAtFixedRate(new TimerTask() {
	    @Override
	    public void run() {
	      try {
	      
		Chord    chord = new Chord(port);
		System.out.println("Usage: \n\tjoin <port>\n\twrite <file> "
                        + "(the file must be an integer stored in the working "
                        + "directory, i.e, ./port/file");
		System.out.println("\tread <file>\n\tdelete <file>\n\tprint");
        
		Scanner scan= new Scanner(System.in);
		String delims = "[ ]+";
		String command = "";
		while (true)
		{
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
				int guid = Integer.parseInt(tokens[1]);
                                chord.write(guid, chord);
//                                
//				// If you are using windows you have to use
//// 				path = ".\\"+  port +"\\"+Integer.parseInt(tokens[1]); // path to file
////                              "./"+i+"/repository/" + guid;
//				String path = "./"+  port +"/repository/"+guid; // path to file
//				FileStream file = new FileStream(path);
//				ChordMessageInterface peer = chord.locateSuccessor(guid);
//				peer.put(guid, file); // put file into ring
			} catch (Exception e) {
				e.printStackTrace();
			}
                        //Transaction t = new Transaction(Operation.WRITE);
                        
		    }
		    if  (tokens[0].equals("read") && tokens.length == 2) {
			try {	
			  int guid = Integer.parseInt(tokens[1]);
			  // If you are using windows you have to use
  // 			path = ".\\"+  port +"\\"+Integer.parseInt(tokens[1]); // path to file
  //                    "./"+i+"/repository/" + guid;
			  String path = "./"+  port +"/repository/"+guid; // path to file
			  FileStream file = new FileStream(path);
			  ChordMessageInterface peer = chord.locateSuccessor(guid);
			  peer.put(guid, file); // put file into ring
			} catch (Exception e) {
				e.printStackTrace();
			} 
		    }
		    if  (tokens[0].equals("delete") && tokens.length == 2) {
			try {
			  chord.delete(Integer.parseInt(tokens[1]));
			} catch (Exception e) {
			      e.printStackTrace();
			}
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
