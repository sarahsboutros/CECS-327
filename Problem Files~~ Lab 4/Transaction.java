import java.math.BigInteger;
import java.io.*;
import java.util.*;
public class Transaction implements Serializable  {
  public enum Operation { WRITE, DELETE}
  public enum Vote { YES, NO }
  long TransactionId;
  Integer guid;
  Operation op;
  Vote vote;
  ChordMessageInterface creator;
  FileStream fileStream;
  Long writtenTime;
  Long readTime;
  public Transaction(Operation op, FileStream file)
  {
  	//id = md5(date + ip+port);
  	this.op = op;
    this.fileStream = file;
    TransactionId = (new Date()).getTime();
  }

  public void setCoordinator(Chord c)
  {
    creator = c;
  }
  public ChordMessageInterface getCoordinator()
  {
    return creator;
  }

  @Override
  public boolean equals(Object obj)
  {
    return(this.TransactionId == ((Transaction)obj).TransactionId);
  }

  @Override
  public int hashCode()
  {
    int t = (int) TransactionId;
    return t;
  }
}
