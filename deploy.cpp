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
        "54.67.65.3",
        "54.76.142.156",
        "54.169.150.46",
        "54.94.187.28"
    };
    
    static const string cert[] = {
        "turtlebeards.pem",
        "turtlebeards_california.pem",
        "turtlebeards_ireland.pem",
        "turtlebeards_singapore.pem",
        "turtlebeards_saopaulo.pem"
    };

    // scp -r -i ~/Desktop/turtlebeards.pem ~/Dropbox/Current\ Documents/cs271/cs271_proj1_java/clientServer/dist/clientServer.jar ec2-user@54.174.167.183:/home/ec2-user/
    for(int i = 0; i < 5; i++) {
        cout << "> DEPLOYING FILES TO REMOTE HOST" << endl;
        string cmd = "scp -r -i ~/Desktop/"+ cert[i] +" ~/Dropbox/Current\\ Documents/cs271/cs271_proj1_java/clientServer/dist/clientServer.jar ec2-user@"+address[i]+":/home/ec2-user/";
        cout.write(cmd.c_str(), strlen(cmd.c_str()));
        cout.put('\n');
        string result = exec(cmd.c_str());

        cout << "> DELETING LOGS" << endl;
        cmd = "ssh -i /Users/wdai/Desktop/"+ cert[i] +" ec2-user@"+address[i]+" 'rm log.txt'";
        cout.write(cmd.c_str(), strlen(cmd.c_str()));
        cout.put('\n');
        result = exec(cmd.c_str());
        // sleep(1);
    }

    exit(EXIT_SUCCESS);
}
