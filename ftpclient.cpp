/**
 C++ client example using sockets
 */
#include <iostream>    //cout
#include <string>
#include <stdio.h> //printf
#include <stdlib.h>
#include <string.h>    //strlen
#include <sys/socket.h>    //socket
#include <arpa/inet.h> //inet_addr
#include <netinet/in.h>
#include <sys/types.h>
#include <unistd.h>
#include <netdb.h>


#define BUFFER_LENGTH 2048

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
        std::cout << "by ip";
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


int request(int sock, std::string message)
{
    char buffer[BUFFER_LENGTH];
    std::string reply;
    return send(sock, message.c_str(), message.size(), 0);
}

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

int main(int argc , char *argv[])
{
    int sockpi;
    std::string strReply;
    
    //TODO  arg[1] can be a dns or an IP address using gethostbyname.
    argv[1] = gethostbyname("130.179.16.134")->h_name;
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
    strReply = requestReply(sockpi, "PASV \r\n");
    std::cout << strReply  << std::endl;
    
    if(strReply.substr(0,3) == "227"){
        std::cout << "#####     Entered passive mode     #####" << std::endl;
        
        //assign PASV return value: "host.p1.p2"
        std::string passiveIp = strReply.substr(strReply.find('(') + 1, (strReply.find(')') - strReply.find('(') - 1));
        std::string tempPort = passiveIp.substr(passiveIp.find("134") + 4);
        
        //get p1 and p2 port information
        std::string p1 = tempPort.substr(0, tempPort.find(","));
        std::string p2 = tempPort.substr(tempPort.find(",") + 1);
        
        //convert p1 and p2 to int
        int p1i = atoi(p1.c_str());
        int p2i = atoi(p2.c_str());
        
        //bitwise shift with an or
        int port = ((p1i << 8 ) | p2i);
        
        std::cout << port << std::endl;
        
        sockpi = createConnection("130.179.16.134", port);
        
        
        
    }
    
    return 0;
}
