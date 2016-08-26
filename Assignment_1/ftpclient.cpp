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
        usleep(1000);
        do {
            count = recv(sock, buffer, BUFFER_LENGTH-1, 0);
            buffer[count] = '\0';
            reply += buffer;
        }while (count ==  BUFFER_LENGTH-1);
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
    }while (count ==  BUFFER_LENGTH-1);
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
    std::string status = strReply.substr(0,3);
//    int code = std::atoi(status)
//    switch (status) {
//        case "":
//            <#statements#>
//            break;
//            
//        default:
//            break;
//    }
    std::cout << status  << std::endl;
    std::cout << strReply  << std::endl;
    
    strReply = requestReply(sockpi, "PASS sarah.boutros@student.csulb.edu\r\n");
    std::cout << strReply  << std::endl;
//    strReply = requestReply(sockpi, "USER anonymous\r\n");
//    std::cout << strReply  << std::endl;
//    //TODO parse srtReply to obtain the status. Let the system act according to the status and display
    // friendly user to the user
    
    
    //TODO implement PASV, LIST, RETR
    
    
    return 0;
}
