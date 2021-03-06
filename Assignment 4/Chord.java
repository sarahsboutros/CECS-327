import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.net.*;
import java.util.*;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
 
public class Chord extends java.rmi.server.UnicastRemoteObject implements ChordMessageInterface, AtomicCommitInterface
{
    public static final int M = 2;
    
    Registry registry;    // rmi registry for lookup the remote objects.
    ChordMessageInterface successor;
    ChordMessageInterface predecessor;
    ChordMessageInterface[] finger;
    int nextFinger;
    int i;   		// GUID
    
    public void write(String f, Chord c) throws IOException, NoSuchAlgorithmException {
	String path = "./"+  c.i +"/repository/"+f; // path to file
	FileStream file = new FileStream(path);
	ChordMessageInterface peer = c.locateSuccessor(Integer.parseInt(f));
	peer.put(md5(f), file); // put file into ring
    }
    public int md5(String s) throws NoSuchAlgorithmException, UnsupportedEncodingException{
        int m = Integer.parseInt(s);
        /*byte[] b = s.getBytes("UTF-8");
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        b = md5.digest(b);
        
        */
        return m;
    }
    public ChordMessageInterface rmiChord(String ip, int port)
    {	
        ChordMessageInterface chord = null;
        try{
            Registry registry = LocateRegistry.getRegistry(ip, port);
            chord = (ChordMessageInterface)(registry.lookup("Chord"));
            return chord;
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch(NotBoundException e){
            e.printStackTrace();
        }
        return null;
    }
    
    public Boolean isKeyInSemiCloseInterval(int key, int key1, int key2)
    {
       if (key1 < key2)
	return (key > key1 && key <= key2);
      else
	return (key > key1 || key <= key2);
    }

    public Boolean isKeyInOpenInterval(int key, int key1, int key2)
    {
      if (key1 < key2)
	return (key > key1 && key < key2);
      else
	return (key > key1 || key < key2);
    }
    
    
    public void put(int guid, InputStream stream) throws RemoteException {
	 //TODO Store the file at ./port/repository/guid
      try {
	 String fileName = "./"+i+"/repository/" + guid;
	 FileOutputStream output = new FileOutputStream(fileName);
	 while (stream.available() > 0)
	    output.write(stream.read());
       } catch (IOException e)
       {
	  System.out.println(e);
       } 
    }
    
    
    public InputStream get(int guid) throws RemoteException {
	 String fileName = "./"+i+"/repository/" + guid;
	 FileStream file= null;
	 try{
	   file = new FileStream(fileName);
	 } catch (Exception e) {
	    e.printStackTrace();
	}
        return file;        
    }
    
    public void delete(int guid) throws RemoteException {
	  String fileName = "./"+i+"/repository/" + guid;

          File file = new File(fileName);
	  file.delete();
    }
    
    public int getId() throws RemoteException {
        return i;
    }
    public boolean isAlive() throws RemoteException {
	    return true;
    }
    
    public ChordMessageInterface getPredecessor() throws RemoteException {
	    return predecessor;
    }
    
    public ChordMessageInterface locateSuccessor(int key) throws RemoteException {
	    if (key == i)
            throw new IllegalArgumentException("Key must be distinct that  " + i);
	    if (successor.getId() != i)
	    {
	      if (isKeyInSemiCloseInterval(key, i, successor.getId()))
	        return successor;
	      ChordMessageInterface j = closestPrecedingNode(key);
	      
          if (j == null)
	        return null;
	      return j.locateSuccessor(key);
        }
        return successor;
    }
    
    public ChordMessageInterface closestPrecedingNode(int key) throws RemoteException {
	int count = M-1;	
	if (key == i)  throw new IllegalArgumentException("Key must be distinct that  " + i);	
	for (count = M-1; count >= 0; count--) {
	  if (finger[count] != null && isKeyInOpenInterval(finger[count].getId(), i, key)) 
	      return finger[count];
        }
        return successor;

    }
    
    public void joinRing(String ip, int port)  throws RemoteException {
        try{
            System.out.println("Get Registry to joining ring");
            Registry registry = LocateRegistry.getRegistry(ip, port);
            ChordMessageInterface chord = (ChordMessageInterface)(registry.lookup("Chord"));
            predecessor = null;
            successor = chord.locateSuccessor(this.getId());
	    System.out.println("Joining ring");
	 }
        catch(RemoteException | NotBoundException e){
            successor = this;
        }   
    }
    
    public void findingNextSuccessor()
    {
	int i;
	successor = this;
	for (i = 0;  i< M; i++)
	{
	  try 
	  {
	      if (finger[i].isAlive())
	      {
		  successor = finger[i];
	      }	    
	  }
	  catch(RemoteException | NullPointerException e)
	  {	    
	      finger[i] = null;
	  }
	}
    }
    
    public void stabilize() {
      boolean error = false;
      try {
	  if (successor != null)
	  {
	    ChordMessageInterface x = successor.getPredecessor();	
	   
	    if (x != null && x.getId() != this.getId() && isKeyInOpenInterval(x.getId(), this.getId(), successor.getId())) 
	    {  
	      successor = x;
	    }
	    if (successor.getId() != getId())
	    {
	      successor.notify(this);
	    }
	  }
      } catch(RemoteException | NullPointerException e1) 
      {
 	  error = true;
      }
      if (error)
 	findingNextSuccessor();
    }
    
    public void notify(ChordMessageInterface j) throws RemoteException {
         if (predecessor == null || (predecessor != null && 
                 isKeyInOpenInterval(j.getId(), predecessor.getId(), i))){
	 // TODO 
	 //transfer keys in the range [j,i) to j;
             Path path = Paths.get("./"
                    + this.getId() + "/repository/" );
            
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path file: stream)
                {
                     if(!(new File(path+file.toString()).isHidden())) {
                    int key = Integer.parseInt(file.getFileName().toString());
                    System.out.println(file.getFileName());
                     
                    
                    if(key > 0 && isKeyInSemiCloseInterval(key,j.getId(),i))
                    {
                        j.put(key,get(key));
                    }
                     }
                }
            } catch (IOException | DirectoryIteratorException x) {
                // IOException can never be thrown by the iteration.
                // In this snippet, it can only be thrown by newDirectoryStream.
                System.err.println(x);
            }
         
         }
             
	 predecessor = j;
    }
    
    public void fixFingers() {
    
        int id= i;
        try {
	    int nextId;
	    if (nextFinger == 0) // && successor != null)
	      nextId = (this.getId() + (1 << nextFinger));	
	    else 
	      nextId = finger[nextFinger -1].getId();
	    finger[nextFinger] = locateSuccessor(nextId);
	    
	    if (finger[nextFinger].getId() == i)
   	      finger[nextFinger] = null;
	    else
	      nextFinger = (nextFinger + 1) % M;
       } catch(RemoteException | NullPointerException e){
	  finger[nextFinger] = null;
 	  e.printStackTrace();
      }
    }
    
    public void checkPredecessor() { 	
      try {
	if (predecessor != null && !predecessor.isAlive())
	      predecessor = null;
      } 
      catch(RemoteException e) 
      {
	  predecessor = null;
//           e.printStackTrace();
      }
    }
       
    public Chord(int port) throws RemoteException {
        int j;
	finger = new ChordMessageInterface[M];
        for (j=0;j<M; j++){
	    finger[j] = null;
	}
        i = port;
	
        predecessor = null;
	successor = this;
	Timer timer = new Timer();
	timer.scheduleAtFixedRate(new TimerTask() {
	    @Override
	    public void run() {
	      stabilize();
	      fixFingers();
	      checkPredecessor();
	    }
	}, 500, 500);
	try{
	// create the registry and bind the name and object.
	    System.out.println("Starting RMI at port="+port);
	    registry = LocateRegistry.createRegistry( port );
	  registry.rebind("Chord", this);
	}
	catch(RemoteException e){
	       throw e;
        } 
    }
    
    void Print()
    {   
	int i;
	try {
	  if (successor != null)
	      System.out.println("successor "+ successor.getId());
	  if (predecessor != null)
	      System.out.println("predecessor "+ predecessor.getId());
	  for (i=0; i<M; i++)
	  {
	    try {
	  	if (finger != null)
		  System.out.println("Finger "+ i + " " + finger[i].getId());
	    } catch(NullPointerException e)
	    {
	      finger[i] = null;
	   }
	  }
       }
        catch(RemoteException e){
	       System.out.println("Cannot retrive id");
        } 
    }

    @Override
    public boolean canCommit(Transaction t) throws RemoteException {
        Long date = new Date().getTime();
        return t.TransactionId.longValue() < date;
    }

    @Override
    public void doCommit(Transaction t) throws RemoteException {
        
    }

    @Override
    public void doAbort(Transaction t) throws RemoteException {
        
    }

    @Override
    public boolean haveCommitted(Transaction t, Transaction p) throws RemoteException {
        return t.TransactionId.longValue() < p.TransactionId.longValue();
    }

    @Override
    public boolean getDecision(Transaction t) throws RemoteException {
        return false;
    }
   
}
