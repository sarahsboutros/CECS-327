import java.rmi.*;
import java.io.*;
import java.util.ArrayList;

public interface ChordMessageInterface extends Remote
{
    public ChordMessageInterface getPredecessor()  throws RemoteException;
    ChordMessageInterface locateSuccessor(int key) throws RemoteException;
    ChordMessageInterface closestPrecedingNode(int key) throws RemoteException;
    public void joinRing(String Ip, int port)  throws RemoteException;
    public void notify(ChordMessageInterface j) throws RemoteException;
    public boolean isAlive() throws RemoteException;
    public int getId() throws RemoteException;
    public void setSuccessor(ChordMessageInterface m) throws RemoteException;
    public void setPredecessor(ChordMessageInterface m) throws RemoteException;

    public int getPeerSize() throws RemoteException;
    public ArrayList<ChordMessageInterface> getPeers() throws RemoteException;

    public void put(int guid, InputStream file) throws IOException, RemoteException;
    public InputStream get(int id) throws IOException, RemoteException;
    public void delete(int id) throws IOException, RemoteException;
    public void leaveRing() throws RemoteException;

    public boolean canCommit(Transaction t, int guid) throws RemoteException,FileNotFoundException;
    public void doCommit(Transaction t, int guid,ChordMessageInterface c) throws RemoteException;
    public void doAbort(Transaction t,int guid) throws RemoteException;
    public void haveCommitted(Transaction t) throws RemoteException;
    public void getDecision(Transaction t, int guid) throws RemoteException;
    public boolean TransactionsCreatedContainsKey(Transaction t) throws RemoteException;
}
