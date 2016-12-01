import java.awt.event.ActionListener;
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
import java.util.logging.Level;
import java.util.logging.Logger;

public class Chord extends java.rmi.server.UnicastRemoteObject implements ChordMessageInterface,Serializable
{
    public static final int M = 2;

    ArrayList<ChordMessageInterface> peers;
    Registry registry;    // rmi registry for lookup the remote objects.
    ChordMessageInterface successor;
    ChordMessageInterface predecessor;
    ChordMessageInterface[] finger;
    HashMap<Transaction,Integer> TransactionsReceived;
    HashMap<Transaction, Integer> TransactionsCreated;
    HashMap<Integer,Long> lastRead;
    HashMap<Integer,Long> lastWritten;

    int nextFinger;
    int i;   		// GUID

    public void write(String f, Chord c) throws IOException, NoSuchAlgorithmException {
        String path = "./"+  c.i +"/workdir/"+f; // path to file
        FileStream file = new FileStream(path);


        //ChordMessageInterface peer = c.locateSuccessor(f);
        int one = md5(f+1) % (peers.size());
        int two = md5(f+2) % (peers.size());
        int three = md5(f+3) %(peers.size());

        Transaction transaction = new Transaction(Transaction.Operation.WRITE,file);
        transaction.creator = this;
        transaction.guid = md5(f) %(peers.size());
        transaction.writtenTime = lastWritten.get(transaction.guid);
        transaction.readTime = lastRead.get(transaction.guid);
        TransactionsCreated.put(transaction, 0);
        //        Chord peerone = (Chord)peers.get(one);
        //        Chord peertwo = (Chord)peers.get(two);
        //        Chord peerthree = (Chord)peers.get(three);
        //
        ChordMessageInterface peerone = c.locateSuccessor(one);
        ChordMessageInterface peertwo = c.locateSuccessor(two);
        ChordMessageInterface peerthree = c.locateSuccessor(three);

        boolean voteone,votetwo,votethree;
        System.out.println(peerone.getId());
   //     peerone.dummy(transaction);
       voteone = peerone.canCommit(transaction,one);
       votetwo = peertwo.canCommit(transaction,two);
       votethree = peerthree.canCommit(transaction,three);

       if(voteone == true
               && votetwo == true
               && votethree == true)
       {
           peerone.doCommit(transaction,one);
           peertwo.doCommit(transaction,two);
           peerthree.doCommit(transaction,three);
       }
       else
       {
               peerone.doAbort(transaction,one);
               peertwo.doAbort(transaction,two);
               peerthree.doAbort(transaction,three);


       }

//peer.put(f, file); // put file into ring
    }
    public InputStream read(String f) throws IOException,NoSuchAlgorithmException
    {
      int guidone = md5(f+1) % (peers.size());
      int guid = md5(f) % (peers.size());
      lastRead.put(guid,(new Date().getTime()));
      		  ChordMessageInterface peer = locateSuccessor(guidone);
            return peer.get(guidone); // put file into ring
    }
    public int md5(String s) throws NoSuchAlgorithmException, UnsupportedEncodingException
    {

//        int smallerNumber = 0;
//		try{
//			MessageDigest md = MessageDigest.getInstance("MD5");
//			byte[] messageDigest = md.digest(s.getBytes());
//			BigInteger bigNumber = new BigInteger(1, messageDigest);
//			BigInteger aMod = new BigInteger("32768");
//			smallerNumber = bigNumber.mod(aMod).intValue();
//		} catch(Exception e){
//			e.printStackTrace();
//			System.out.println("Could not put file!");
//		}
//		return smallerNumber;
        int m;
        byte[] b = s.getBytes("UTF-8");
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        m = (new BigInteger(md5.digest(b))).intValue();

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


    public InputStream get(int guid) throws RemoteException
    {
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

    public int getPeerSize() throws RemoteException {
        return peers.size();
    }

    public ArrayList<ChordMessageInterface> getPeers() throws RemoteException
    {
        return peers;
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
        successor.setPredecessor(predecessor);

    }

    public void setPredecessor(ChordMessageInterface m)
    {
        this.predecessor = m;
    }

    public void setSuccessor(ChordMessageInterface m)
    {
        this.successor = m;
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
            if(!peers.contains(finger[nextFinger]) && finger[nextFinger]!=null && finger[nextFinger] != this)
            {
                peers.add(finger[nextFinger]);
            }
            else if(!peers.contains(successor) && successor != null && successor!=this)
            {
                peers.add(successor);
            }
            else if(!peers.contains(predecessor) && predecessor != null && predecessor!=this)
            {
                peers.add(predecessor);
            }

            if(successor.getPeerSize() > peers.size())
            {
                for(int i=0; i< successor.getPeerSize(); i++)
                {
                    if(!peers.contains(successor.getPeers().get(i))
                            && successor.getPeers().get(i)!= this)
                    {
                        peers.add(successor.getPeers().get(i));
                    }
                    if(peers.get(i).getId() == this.getId())
                    {
                        peers.remove(i);
                    }

                }
            }
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

    public Chord(int port) throws RemoteException,IOException,ClassNotFoundException {
        int j;
        System.out.println("");
        finger = new ChordMessageInterface[M];
        peers = new ArrayList<ChordMessageInterface>();
        TransactionsReceived = new HashMap<Transaction,Integer>();
        TransactionsCreated = new HashMap<Transaction,Integer>();
        String pathread = "./"+port+"/system/read";
        String pathwrite = "./"+port+"/system/write";
        ObjectInputStream read = new ObjectInputStream(new FileInputStream(pathread));
        lastRead = (HashMap<Integer,Long>)read.readObject();

        ObjectInputStream write = new ObjectInputStream(new FileInputStream(pathwrite));
        lastWritten = (HashMap<Integer,Long>)write.readObject();

        for (j=0;j<M; j++)
        {
            finger[j] = null;
        }
        i = port;
        System.out.println(port);
        predecessor = null;
        successor = this;
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                stabilize();
                checkPredecessor();
                fixFingers();
                // for (Map.Entry<String, Transaction> entry : TransactionsReceived.entrySet())
                // {
                //     try {
                //         getDecision(entry.getValue());
                //     } catch (RemoteException ex) {
                //         Logger.getLogger(Chord.class.getName()).log(Level.SEVERE, null, ex);
                //     }
                // }
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

     protected void finalize() throws FileNotFoundException,IOException
     {
       String pathread = "./"+i+"/system/read";
       String pathwrite = "./"+i+"/system/write";

       ObjectOutputStream read = new ObjectOutputStream(new FileOutputStream(pathread));
       read.writeObject(lastRead);

       ObjectOutputStream write = new ObjectOutputStream(new FileOutputStream(pathwrite));
       write.writeObject(lastWritten);
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

            for(i = 0; i < peers.size(); i++)
            {
                try{
                    System.out.println(peers.get(i).getId());


                } catch(NullPointerException e)
                {

                }
                System.out.println(peers.size());
//              if(peers.get(i) == null)
//              {
//                  peers.remove(i);
//              }
            }
            System.out.println("Succ"+successor.getPeerSize());
            System.out.println("Me"+peers.size());
        }
        catch(RemoteException e){
            System.out.println("Cannot retrive id");
        }
    }

    @Override
    public boolean canCommit(Transaction t,int guid) throws RemoteException,FileNotFoundException
    {
//        Long date = new Date().getTime();
        try {
            String fileName = "./"+i+"/temp/"+t.guid;
            FileOutputStream output = new FileOutputStream(fileName);
            while (t.fileStream.available() > 0)
                output.write(t.fileStream.read());
        } catch (IOException e)
        {
            System.out.println(e);
        }


        String pathtocheck = "./"+i+"/repository/" + guid;
        long lR = lastRead.get(t.guid);
        long lW = lastWritten.get(t.guid);


        if (!TransactionsReceived.containsKey(pathtocheck) ||
        ((lW < t.readTime) && (lW < t.writtenTime)))
        {
            TransactionsReceived.put(t,guid);
            return true;
        }
        else
        {

            return false;
        }


    }

    @Override
    public void doCommit(Transaction t,int guid) throws RemoteException
    {
      put(guid, t.fileStream);
      lastWritten.put(t.guid,(new Date()).getTime());
        String fileName = "./"+i+"/repository/" + t.guid;
        try {

            FileOutputStream output = new FileOutputStream(fileName);
            while (t.fileStream.available() > 0)
                output.write(t.fileStream.read());
        } catch (IOException e)
        {
            System.out.println(e);
        }

        TransactionsReceived.remove(fileName, t);
        haveCommitted(t);
    }

    @Override
    public void doAbort(Transaction t,int guid) throws RemoteException
    {
        String fileName = "./"+i+"/temp/"+t.guid;
        File file = new File(fileName);
        file.delete();
        TransactionsReceived.remove(t);
    }

    @Override
    public void haveCommitted(Transaction t) throws RemoteException {
        Chord coordinator = t.creator;

        int count = coordinator.TransactionsCreated.get(t);



        if(count+1 >=3)
        {
            coordinator.TransactionsCreated.remove(t);
        }
        else
        {
            coordinator.TransactionsCreated.replace(t,count + 1);
        }
        // return t.TransactionId.longValue() < p.TransactionId.longValue();
    }

    @Override
    public void getDecision(Transaction t,int guid) throws RemoteException
    {
        Chord  coordinator = t.creator;
        if(coordinator.TransactionsCreated.containsKey(t))
        {
            if(t.vote == Transaction.Vote.YES)
            {
                this.doCommit(t,guid);
            }
            else
            {
                this.doAbort(t,guid);

            }
        }
        else
        {
            this.doAbort(t,guid);
        }
    }
    public boolean dummy(Transaction t)
    {
        return true;
    }

}
