
import java.rmi.Remote;
import java.rmi.RemoteException;


public interface ShutdownInterface extends Remote { 
    /// Catches the Control + C command
    public void shutdown() throws RemoteException;
}