
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Shutdown extends Thread implements Runnable {

    public ShutdownInterface shutdown;

    public Shutdown(ShutdownInterface shutdown) {
        this.shutdown = shutdown;
    }
    /// Catches the Control + C command
    public void run() {
        try {
            this.shutdown.shutdown();
        } catch (RemoteException ex) {
            System.out.println("Caught Shutdown fail");
            Logger.getLogger(Shutdown.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}