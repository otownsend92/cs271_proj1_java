all: deploy.cpp
	g++ -g -w -Wall -m32 -o deploy deploy.cpp
	g++ -g -w -Wall -m32 -o modpaxdeploy modpaxdeploy.cpp
clean:
	$(RM) deploy
	$(RM) modpaxdeploy
