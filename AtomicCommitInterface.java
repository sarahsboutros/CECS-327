import java.rmi.*;
import java.rmi.RemoteException;


public interface AtomicCommitInterface extends Remote {
    public boolean canCommit(Transaction t) throws RemoteException;
    public void doCommit(Transaction t) throws RemoteException;
    public void doAbort(Transaction t) throws RemoteException;
    public boolean haveCommitted(Transaction t, Transaction p) throws RemoteException;
    public boolean getDecision(Transaction t) throws RemoteException;
}
/*
canCommit?(trans) → Y es/No: Call from coordinator to participant to ask 
whether it can commit a transaction. Participant replies with its vote.

doCommit(trans): Call from coordinator to participant to tell partic- ipant 
to commit its part of a transaction.

doAbort(trans): Call from coordinator to participant to tell participant 
to abort its part of a transaction.

haveCommitted(trans,participant): Call from participant to coordi- nator 
to confirm that it has committed the transaction.

getDecision(trans) → Y es/N o: Call from participant to coordinator 
to ask for the decision on a transaction when it has voted Yes but 
has still had no reply after some delay. Used to recover from 
server crash or delayed messages.
*/