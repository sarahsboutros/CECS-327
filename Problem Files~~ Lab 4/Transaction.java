import java.math.BigInteger;
import java.io.*;
import java.util.*;
public class Transaction implements Serializable  {
  public enum Operation { WRITE, DELETE}
  public enum Vote { YES, NO }
  Long TransactionId;
  Integer guid;
  Operation op;
  Vote vote;
  FileStream fileStream;
  Chord creator;
  Long writtenTime;
  Long readTime;
  public Transaction(Operation op, FileStream file)
  {
  	//id = md5(date + ip+port);
  	this.op = op;
    this.fileStream = file;
    TransactionId = (new Date()).getTime();
  }

}
