//package chat;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
/*****************************//**
* \brief It implements a distributed chat. 
* It creates a ring and delivers messages
* using flooding 
**********************************/
public class Chat implements Serializable {
    Peer pred;
    Peer suc;
  public enum enum_MSG {
      JOIN,
      ACCEPT,
      ACCEPTED,
      LEAVE,
      PUT;
   };
  
  public class Peer implements Serializable
    {
      String id, ip;
      int port;
      public Peer() {
      }
      public Peer(String id, String ip, int port) {
          this.id = id;
          this.ip = ip;
          this.port = port;
      }
      public Peer(String id, int port) {
          this.id = id;
          this.port = port;
      }
  }
  

  
 public class MainMessage implements Serializable  {
    enum_MSG messageID;
    String id, id_pred, id_suc, idSender, idDest;
    String ip, ip_pred,ip_suc;
    String text;
    int port, port_pred, port_suc, portDest;
    
    public MainMessage() {
    }
    
    public MainMessage(enum_MSG m) {
        this.messageID = m;
    }
    
  }
 
/*****************************//**
* \class Server class "chat.java" 
* \brief It implements the server
**********************************/ 
  public class Server implements Runnable
  {
    String id, ip;
    int port;
    public Server(String id, int port)
    {
       this.port = port;
       this.id = id;
    }
/*****************************//**
* \brief It allows the system to interact with the participants. 
**********************************/   
    //server run override
    public void run() {
        try {
            ServerSocket servSock = new ServerSocket(port);
            while (true)
            {
                Socket clntSock = servSock.accept(); // Get client connections
                ObjectInputStream  ois = new
                  ObjectInputStream(clntSock.getInputStream());
                ObjectOutputStream oos = new
                  ObjectOutputStream(clntSock.getOutputStream());
                try {
                    MainMessage m = (MainMessage)ois.readObject();
                    //get ip from client socket and set to message
                    m.ip = clntSock.getInetAddress().getHostAddress();
                    //get port
                    //m.port = clntSock.getPort();  // dont need
                    // Handle Messages
                    switch(m.messageID) {
                        case JOIN:
                            System.out.println("Recieving msg id: " + m.id);
                            System.out.println("Recieving msg port: " + m.port);
                            //Peer p = new Peer(m.id, m.port);
                            
                            MainMessage join = new MainMessage(enum_MSG.ACCEPT);
                            join.id = this.id;
                            join.port = this.port;
                            join.idDest = m.id;
                            join.portDest = m.port;
                            join.id_pred = pred.id;
                            join.port_pred = pred.port;
                            pred.port = m.port;
                            pred.id = m.id;
                            Socket socketJ = new Socket("127.0.0.1", join.portDest);
                            ObjectOutputStream oj = new
                                ObjectOutputStream(socketJ.getOutputStream());
                            oj.writeObject(join);
                            socketJ.close();
                            
                            break;
                        case ACCEPT:
                            System.out.println("Accepting");
                            System.out.println("Recieving msg id: " + m.id);
                            System.out.println("Recieving msg port: " + m.port);
                            suc.id = m.id;
                            suc.port = m.port;
                            pred.id = m.id_pred;
                            pred.port = m.port_pred;
                            System.out.println(this.id);
                            System.out.println(this.port);
                            
                            MainMessage accept = new MainMessage(enum_MSG.ACCEPTED);
                            accept.id = this.id;
                            accept.port = this.port;
                            accept.portDest = pred.port;
                            System.out.println(pred.port);
                            
                            Socket socketA = new Socket("127.0.0.1", accept.portDest);
                            ObjectOutputStream oa = new
                                ObjectOutputStream(socketA.getOutputStream());
                            oa.writeObject(accept);
                            socketA.close();
                            
                            break;
                            
                        case ACCEPTED:
                            suc.id = m.id;
                            suc.port = m.port;
                            
                            System.out.println("Successor id and port");
                            System.out.println(suc.id);
                            System.out.println(suc.port);
                            System.out.println("Predecessor id and port");
                            System.out.println(pred.id);
                            System.out.println(pred.port);
                            
                            break;
                        case PUT:
                            if((m.idDest).equalsIgnoreCase(this.id)) {
                                
                                System.out.println("Message: " + m.text);
                            }
                            else if((this.id).equalsIgnoreCase(m.idSender)) {
                                System.out.println("User not found");
                                break;
                            }
                            else
                            {
                                System.out.println("Sending it along");
                                Socket socketP = new Socket("127.0.0.1", suc.port);
                                ObjectOutputStream po = new
                                ObjectOutputStream(socketP.getOutputStream());
                                po.writeObject(m);
                                socketP.close();
                            }
                            break;
                        case LEAVE:
                            if((m.id).equalsIgnoreCase(pred.id))
                            {
                                pred.id = m.id_pred;
                                pred.port = m.port_pred;
                                System.out.println("My new predecessor is: "+pred.id);
                            
                            }
                            else if((m.id).equalsIgnoreCase(suc.id))
                            {
                                suc.id = m.id_suc;
                                suc.port = m.port_suc;
                                System.out.println("My new successor is: "+suc.id);
                            }
                        default:
                            break;
                    }
                    
                    
                    
                    
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger
                        (Chat.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                clntSock.close();
            } 
        } catch (IOException ex) {
            Logger.getLogger(Chat.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
  }
  
//class Client class "chat.java" 
/*****************************//*
* \brief It implements the client
**********************************/
  public class Client implements Runnable
  {       
    String id, ip;
    int port;
    String ipConnect;
    int portConnect;
    MainMessage m = new MainMessage(); 
    //PUT command variables
    String idSender, idDest, text;
    
    public Client(String id, int p)
    {
       this.port = p;
       this.id = id;
    }

  /*****************************//**
* \brief It allows the user to interact with the system. 
**********************************/    
    //client run override
    public void run()
    {
      while (true)
      {
          try {
              //Get the message information from the User
              Scanner in = new Scanner(System.in);
              System.out.println("Input command: JOIN, PUT, LEAVE");
              String cmd = in.nextLine();
              
              if(cmd.equalsIgnoreCase("JOIN")) {
                  //System.out.println("Enter the ID");
                  //id = in.nextLine();
                  System.out.println("Enter the IP");
                  ipConnect = in.nextLine();
                  System.out.println("Enter the port");
                  portConnect = in.nextInt();
                  MainMessage mm = new MainMessage(enum_MSG.JOIN);
                  mm.id = this.id;
                  mm.port = this.port;
                  
              Socket socket = new Socket(ipConnect, portConnect);
              ObjectOutputStream oos = new
                  ObjectOutputStream(socket.getOutputStream());
              ObjectInputStream ois = new
                  ObjectInputStream(socket.getInputStream());
              oos.writeObject(mm);
              //ois.read();
              socket.close();
              }
              
             else if(cmd.equalsIgnoreCase("PUT")) {
                  //System.out.println("Enter the Source");
                  //idSender = in.nextLine();
                  System.out.println("Enter the Dest");
                  idDest = in.nextLine();
                  System.out.println("Enter the text");
                  text = in.nextLine();
                  MainMessage mm = new MainMessage(enum_MSG.PUT);
                  mm.idSender = this.id;
                  mm.idDest = idDest;
                  mm.text = text;
                 // mm.Message();
                 Socket socket = new Socket("127.0.0.1",suc.port);
              ObjectOutputStream oos = new
                  ObjectOutputStream(socket.getOutputStream());
              ObjectInputStream ois = new
                  ObjectInputStream(socket.getInputStream());
              oos.writeObject(mm);
              //ois.read();
              socket.close();
              }
              
              else if(cmd.equalsIgnoreCase("LEAVE")) {
                  MainMessage sucm = new MainMessage(enum_MSG.LEAVE);
                  sucm.id = this.id;
                  sucm.id_pred = pred.id;
                  sucm.port_pred = pred.port;
                  
                  Socket socket = new Socket("127.0.0.1",suc.port);
                  ObjectOutputStream oos = new
                  ObjectOutputStream(socket.getOutputStream());
                  ObjectInputStream ois = new
                  ObjectInputStream(socket.getInputStream());
                  oos.writeObject(sucm);
                  socket.close();
                  
                      ///
                  MainMessage predm = new MainMessage(enum_MSG.LEAVE);
                  predm.id = this.id;
                  predm.id_suc = suc.id;
                  predm.port_suc = suc.port;
                  
                  Socket sockettwo = new Socket("127.0.0.1",pred.port);
                  ObjectOutputStream oostwo = new
                  ObjectOutputStream(sockettwo.getOutputStream());
                  ObjectInputStream oistwo = new
                  ObjectInputStream(sockettwo.getInputStream());
                  oostwo.writeObject(predm);
                      //ois.read();
                  sockettwo.close();
                  System.out.println("Exiting");
                  System.exit(0);
                  //mm.Message();
             }
//              Socket socket = new Socket(ip, port);
//              ObjectOutputStream oos = new
//                  ObjectOutputStream(socket.getOutputStream());
//              ObjectInputStream ois = new
//                  ObjectInputStream(socket.getInputStream());
//              oos.writeObject(m);
//              ois.read();
//              socket.close();
          } catch (IOException ex) {
              Logger.getLogger
                    (Chat.class.getName()).log(Level.SEVERE, null, ex);
          }
      }
    }
  }
  
/*****************************//**
* Starts the threads with the client and server:
* \param Id unique identifier of the process
* \param port where the server will listen
**********************************/  
  public Chat(String Id, int port) {
     
      // Initialization of the peer
      //! add server parameter Id
      Thread server = new Thread(new Server(Id, port));
      Thread client = new Thread(new Client(Id, port));
      pred = new Peer(Id,port);
      suc = new Peer(Id,port);
      server.start();
      client.start();
      try {
          client.join();
          server.join();
      } catch (InterruptedException interruptedException) {
      }
  }
  
  public static void main(String[] args) {
      if (args.length < 2 ) {  
          throw new IllegalArgumentException("Parameter: <id> <port>");
      }
      Chat chat = new Chat(args[0], Integer.parseInt(args[1]));
      //String a = "127.0.0.1";
      //String b = "10.39.91.122";
      //Chat chat = new Chat(a, 1025);
      
  }
}
