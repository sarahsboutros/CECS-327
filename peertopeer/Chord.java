import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.net.*;
import java.util.*;
import java.io.*;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.MarshalException;
import java.io.FileInputStream;
 
public class Chord extends java.rmi.server.UnicastRemoteObject implements ChordMessageInterface, ShutdownInterface
{
    public static final int M = 2;
    
    Registry registry;    /// rmi registry for lookup the remote objects.
    ChordMessageInterface successor;
    ChordMessageInterface predecessor;
    ChordMessageInterface[] finger;
    int nextFinger;
    int i;   
    
    
    private Shutdown shutdown;


    ///Makes the ChordMessageInterface object accessable remotely 
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
    
    /// Checks if the Key is in a Semi Close interval
    public Boolean isKeyInSemiCloseInterval(int key, int key1, int key2)
    {
       if (key1 < key2)
	return (key > key1 && key <= key2);
      else
	return (key > key1 || key <= key2);
    }

    /// Checks if the key is in Open Interval 
    public Boolean isKeyInOpenInterval(int key, int key1, int key2)
    {
      if (key1 < key2)
	return (key > key1 && key < key2);
      else
	return (key > key1 || key < key2);
    }
    
    /// Gets the file and puts it in my repository
    public void put(int guid, InputStream file) throws RemoteException, FileNotFoundException, IOException {
	 //TODO Store the file at ./port/repository/guid
         FileOutputStream o;
         String path = "./" 
                 + this.getId() + "/repository/" + guid;
         o = new FileOutputStream(path);
         while(file.available() > 0) {
         o.write(file.read());
         }
    }
    
    /// Retrieves the file from my repository
    public InputStream get(int guid) throws RemoteException, FileNotFoundException, IOException {
         String path = "./" 
                 + this.getId() + "/repository/" + guid;
         //todo get  the file ./port/repository/guid
         FileStream f = new FileStream(path);
        return f;       
    }
    /// Deletes the file form my repository
    public void delete(int guid) throws RemoteException {
          //todo delet the file ./port/repository/guid
         String path = "./" 
                 + this.getId() + "/repository/" + guid;
          File f = new File(path);
          if(f.exists()) {
              f.delete();
          }
    }
    
    /** Returns my Id **/
    public int getId() throws RemoteException {
        return i;
    }

    /// Returns if I'm alive
    public boolean isAlive() throws RemoteException {
	    return true;
    }
    
    /// Returns predecessor
    public ChordMessageInterface getPredecessor() throws RemoteException {
	    return predecessor;
    }
    
    /// Locates successor
    public ChordMessageInterface locateSuccessor(int key) throws RemoteException {
	if (key == i)
            throw new IllegalArgumentException("Key must be distinct that  " + i);
	if (successor.getId() != i) {
	      if (isKeyInSemiCloseInterval(key, i, successor.getId()))
	        return successor;
	      ChordMessageInterface j = closestPrecedingNode(key);
	      
              if (j == null)
	        return null;
	      return j.locateSuccessor(key);
        }
        return successor;
    }
    
    ///Locates closest Preceding Node
    public ChordMessageInterface closestPrecedingNode(int key) throws RemoteException {
	 if (key == i)
            throw new IllegalArgumentException("Key must be distinct that " + i);
        if(predecessor.getId() != i)
        {
            if (isKeyInSemiCloseInterval(key,predecessor.getId(), i))
            {
                return predecessor;
            }
            else {
                ChordMessageInterface j = new Chord(key);
                notify(j);
                predecessor.closestPrecedingNode(key);
            }
        }
        return predecessor;

    }
    
    /// Joins the ring
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
    /// Leaves the ring
    public void leaveRing() throws RemoteException
    {
        Path path = Paths.get("./"
                    + this.getId() + "/repository/" );
            
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path file: stream)
                {
                     if(!(new File(path+file.toString()).isHidden())) {
                    int key = Integer.parseInt(file.getFileName().toString());
                    System.out.println(file.getFileName());
                     
                    
                    if(key > 0)
                    {
                        successor.put(key,get(key));
                    }
                     }
                }
            } catch (IOException | DirectoryIteratorException x) {
                // IOException can never be thrown by the iteration.
                // In this snippet, it can only be thrown by newDirectoryStream.
                System.err.println(x);
            }
        predecessor.setSuccessor(successor);
        successor.setPredeccesor(predecessor);
    } 

    /// Sets Predecessor
    public void setPredeccesor(ChordMessageInterface m)
    {
        this.predecessor = m;
    }

    /// Sets Successor
    public void setSuccessor(ChordMessageInterface m)
    {
        this.successor = m;
    }

    /// Find Next Successor
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
    
    ///Stabilizes the successor and make sure that files in the repository are up to date
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
    
    /// Notify checks if my predecessor is null or if its an open interval with j and i if it is true for either then it copies those files to j's repository

    public void notify(ChordMessageInterface j) throws RemoteException {
         if (predecessor == null || (predecessor != null && isKeyInOpenInterval(j.getId(), predecessor.getId(), i)))
        {    // TODO
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
    
    /// Fixes the fingers so that they are up to date
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
    /// Check Predecessor is alive otherwise set it to null
    public void checkPredecessor() { 	
      try {
	if (predecessor != null && !predecessor.isAlive())
	      predecessor = null;
      } 
      catch(RemoteException e) 
      {
	  predecessor = null;
//         e.printStackTrace();
      }
    }
       
       /// Chord Constructor
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
	/// create the registry and bind the name and object.
	    System.out.println("Starting RMI at port="+port);
	    registry = LocateRegistry.createRegistry( port );
	  registry.rebind("Chord", this);
	}
	catch(RemoteException e){
	       throw e;
        }
        
        shutdown = new Shutdown(this);
        Runtime.getRuntime().addShutdownHook(shutdown);  
    }
    
    /// Shutdown Function for Control + C command
    public void shutdown() throws RemoteException {
        this.leaveRing();
    }
    
    /// Prints out Successor and Predecessor and Fingers
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
   
}
