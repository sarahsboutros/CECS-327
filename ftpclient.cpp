/*
 Sarah Boutros
 Mehmet Ustun
 Evan Medley
 */
#include <iostream>    //cout
#include <string>
#include <sstream>
#include <stdio.h> //printf
#include <stdlib.h>
#include <string.h>    //strlen
#include <sys/socket.h>    //socket
#include <arpa/inet.h> //inet_addr
#include <netinet/in.h>
#include <sys/types.h>
#include <unistd.h>
#include <netdb.h>
/** 
 Buffer Length 
 */

#define BUFFER_LENGTH 2048
/**
 Creates a connection with the Host
 */
int createConnection(std::string host, int port)
{
    int sock;
    struct sockaddr_in sockaddr;
    
    memset(&sockaddr,0, sizeof(sockaddr));
    sock = socket(AF_INET,SOCK_STREAM,0);
    sockaddr.sin_family=AF_INET;
    sockaddr.sin_port= htons(port);
    
    int a1,a2,a3,a4;
    if (sscanf(host.c_str(), "%d.%d.%d.%d", &a1, &a2, &a3, &a4 ) == 4)
    {
        std::cout << "by ip\n";
        sockaddr.sin_addr.s_addr =  inet_addr(host.c_str());
    }
    else {
        std::cout << "by name";
        hostent * record = gethostbyname(host.c_str());
        in_addr * addressptr = (in_addr *) record->h_addr;
        sockaddr.sin_addr = *addressptr;
    }
    if(connect(sock,(struct sockaddr *)&sockaddr,sizeof(struct sockaddr))==-1)
    {
        perror("connection fail");
        exit(1);
        return -1;
    }
    return sock;
}

/**
 Requests a reply from the host
 */
std::string requestReply(int sock, std::string message)
{
    char buffer[BUFFER_LENGTH];
    std::string reply;
    int count = send(sock, message.c_str(), message.size(), 0);
    if (count > 0)
    {
        usleep(100000);
        do {
            count = recv(sock, buffer, BUFFER_LENGTH-1, 0);
            buffer[count] = '\0';
            reply += buffer;
        }while (count == 0);  //BUFFER_LENGTH-1);
    }
    return buffer;
}


/**
 Sends a request to the Host
 */
int request(int sock, std::string message)
{
    char buffer[BUFFER_LENGTH];
    std::string reply;
    return send(sock, message.c_str(), message.size(), 0);
}

/**
 Sends a Reply to the Host
 */
std::string reply(int sock)
{
    std::string strReply;
    int count;
    char buffer[BUFFER_LENGTH];
    
    do {
        count = recv(sock, buffer, BUFFER_LENGTH-1, 0);
        buffer[count] = '\0';
        strReply += buffer;
    }while (count ==  0); // BUFFER_LENGTH-1);
    return strReply;
}

/**
 Enters Passive Mode so Client can receive information
 */
int PASV(std::string host, int sockpi)
{
    std::string strReply = requestReply(sockpi, "PASV \r\n");
    int port,sockdtp;
    if(strReply.substr(0,3) == "227"){
        std::cout << "#####     Entered passive mode     #####" << std::endl;

        std::string passiveIp = strReply.substr(strReply.find('(') + 1, (strReply.find(')') - strReply.find('(') - 1));
        int a1,a2,a3,a4,a5,a6;
        sscanf(passiveIp.c_str(), "%d,%d,%d,%d,%d,%d", &a1, &a2, &a3, &a4, &a5, &a6);
        port = (( a5<< 8 ) | a6);
        sockdtp = createConnection(host, port);
    }
    return sockdtp;
}

/**
 Executes command user inputs
 */
std::string executeCommand(std::string command,std::string host, int sockpi )
{
    std::string strReply,strReply_d;
    int sockdtp;
    sockdtp = PASV(host,sockpi);
    strReply = requestReply(sockpi,command);
    std::cout << strReply << std::endl;
    strReply_d = reply(sockdtp);
    std::cout << strReply << std::endl;
    close(sockdtp);
    strReply = reply(sockpi);
    std::cout << strReply << std::endl;
    return strReply_d;
}



/**
 Main
 */
int main(int argc , char *argv[])
{
    int sockpi;
    std::string strReply;
    std::string host = "130.179.16.134";
    //TODO  arg[1] can be a dns or an IP address using gethostbyname.
    argv[1] = gethostbyname(host.c_str())->h_name;
    if (argc > 2)
    {
        sockpi = createConnection(argv[1], atoi(argv[2]));
    }
    if (argc == 2)
        sockpi = createConnection(argv[1], 21);
    else
        sockpi = createConnection("130.179.16.134", 21);
    strReply = reply(sockpi);
    std::cout << strReply  << std::endl;
    
    
    strReply = requestReply(sockpi, "USER anonymous\r\n");
    //TODO parse srtReply to obtain the status. Let the system act according to the status and display
    // friendly user to the user
    std::cout << strReply  << std::endl;
    
    if(strReply.substr(0,3) == "331"){
        std::cout << "#####     Username accepted     #####" << std::endl;
    }
    
    strReply = requestReply(sockpi, "PASS asa@asas.com\r\n");
    std::cout << strReply  << std::endl;
    //strReply = requestReply(sockpi, "USER anonymous\r\n");
    //TODO parse srtReply to obtain the status. Let the system act according to the status and display
    // friendly user to the user
    if(strReply.substr(0,3) == "230"){
        std::cout << "#####     Password accepted - User is now logged in     #####" << std::endl;
    }
    
    //TODO implement PASV, LIST, RETR
//    std::cout << executeCommand("LIST pub\r\n",host,sockpi)<< std::endl;
//    std::cout << executeCommand("RETR welcome.msg\r\n",host,sockpi) << std::endl;

    int choice;
    std::string input;
    
    do
	{
        std::cout << "1- List a directory"<< std::endl;
        std::cout << "2- Retrieve a file"<< std::endl;
        std::cout << "3- Exit the program "<< std::endl;
        std::cout << "Please enter your choice numerically" << std::endl;
        std::cin >> choice;
        
        if(choice == 1)
        {
            std::cout << "Which a directory do you want to list?" << std::endl;
            std::cin >> input;
            std::string list = "LIST ";
            list.append(input);
            list.append("\r\n");
            std::cout << "Here is the list" << std::endl;
            std::cout << executeCommand(list,host,sockpi)<< std::endl;
        }
        else if(choice == 2)
        {
            
            std::cout << executeCommand("LIST \r\n",host,sockpi)<< std::endl;
            std::cout << "Which file would you like to retrieve?" << std::endl;
            std::cin >> input;
            std::string retr = "RETR ";
            retr.append(input);
            retr.append("\r\n");
            std::cout << executeCommand(retr,host,sockpi) << std::endl;
        }
        else if(choice == 3)
        {
            std::cout << "Program Terminated "<< std::endl;
            strReply = requestReply(sockpi, "QUIT\r\n");
            std::cout << strReply << std::endl;
        }
        else
        {
            std::cout << "Enter a valid choice "<< std::endl;
        }
     } while (choice != 3);

	return 0;
}
