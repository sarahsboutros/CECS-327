import java.math.BigInteger;
import java.io.*;

public class Transaction implements Serializable  {
  public enum Operation { WRITE, DELETE}

  BigInteger TransactionId;
  Integer guid;
  Operation op;
  byte vote;
  FileStream fileStream;   
  public Transaction(Operation op)
  {
  	//id = md5(date + ip+port);
  	this.op = op;
  }  

}
