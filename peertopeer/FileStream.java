import java.io.*;
import java.nio.*;

public class FileStream extends InputStream implements Serializable {
  
    private int currentPosition;
    private byte[] byteBuffer;
    private int size;

    /// Constructor for FileStream that reads the file based on the parameter given
    public  FileStream(String pathName) throws FileNotFoundException, IOException    {
      File file = new File(pathName);
      size = (int)file.length();
      byteBuffer = new byte[size];
      
      FileInputStream fileInputStream = new FileInputStream(pathName);
      int i = 0;
      while (fileInputStream.available()> 0)
      {
	byteBuffer[i++] = (byte)fileInputStream.read();
      }
      fileInputStream.close();	
      currentPosition = 0;	  
    }
    /// Default Constructor for FileStream
    public  FileStream() throws FileNotFoundException    {
      currentPosition = 0;	  
    }
    
    /// Reads the file if the current position is less than the size
    public int read() throws IOException
    {
 	if (currentPosition < size)
 	  return (int)byteBuffer[currentPosition++];
 	return 0;
    }
    
    ///Checks that the current position has not exceeded the size of the file
    public int available() throws IOException
    {
	return size - currentPosition;
    }
}