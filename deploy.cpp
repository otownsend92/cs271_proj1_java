#include <sys/types.h>
#include <sys/stat.h>
#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <unistd.h>
#include <syslog.h>
#include <string.h>

#include <ctime>
#include <iostream>
#include <string>
#include <cstdio>
#include <iostream>
#include <fstream>
using namespace std;

// Exec function
std::string exec(const char* cmd) {
    FILE* pipe = popen(cmd, "r");
    if (!pipe) return "ERROR";
    char buffer[128];
    std::string result = "";
    while(!feof(pipe)) {
        if(fgets(buffer, 128, pipe) != NULL)
            result += buffer;
    }
    pclose(pipe);
    return result;
}

int main(void) {
            
    // init addrs
    static const string address[] = {
        "54.174.167.183", 
        "54.174.226.59", 
        "54.86.223.159", 
        "54.174.201.123", 
        "54.174.164.18"
    };

    // scp -r -i ~/Desktop/turtlebeards.pem ~/Dropbox/Current\ Documents/cs271/cs271_proj1_java/clientServer/dist/clientServer.jar ec2-user@54.174.167.183:/home/ec2-user/
    for(int i = 0; i < 5; i++) {
        string cmd = "scp -r -i ~/Desktop/turtlebeards.pem ~/Dropbox/Current\\ Documents/cs271/cs271_proj1_java/clientServer/dist/clientServer.jar ec2-user@"+address[i]+":/home/ec2-user/";
        cout.write(cmd.c_str(), strlen(cmd.c_str()));
        cout.put('\n');
        string result = exec(cmd.c_str());
        // sleep(1);
    }

    exit(EXIT_SUCCESS);
}
