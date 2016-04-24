#include <stdio.h>
#include <stdlib.h>

int main(int argc, char** argv){
	if(argc < 4){
		printf("%s\n", "Usage: App_diff <ip> <port> <message>");
		exit(1);
	}

	char *ip = argv[1];
	short port = atoi(argv[2]);
	char *msg = argv[3];

	printf("%s %d %s\n", ip, port, msg);

	int sock = socket(PF_INET, SOCK_DGRAM, 0);
	struct addrinfo *first_info;
	struct addrinfo hints;

	memset(&hints, 0, sizeof(struct addrinfo));
	hints.ai_family = AF_INET;
	hints.ai_socktype=SOCK_DGRAM;

	int r = getaddrinfo(ip, port, &hints, &first_info);
	if(r == 0){
		if(first_info != NULL){
			struct sockaddr *saddr = first_info->ai_addr;
			char buff[512];

			strcpy(buff, msg);
			char entier[3];
			sprintf(entier,"%d",i);
			strcat(buff, entier);
			sendto(sock, buff, strlen(buff), 0, saddr(socklen_t)sizeof(struct sockaddr_in));
		}
	}

	return 0;
}