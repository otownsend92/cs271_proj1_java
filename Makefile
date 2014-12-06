all: deploy.cpp
	g++ -g -w -Wall -m32 -o deploy deploy.cpp
clean:
	$(RM) deploy
