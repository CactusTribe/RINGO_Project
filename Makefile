JAVAC=javac
JVM = java

SRC_PATH = src/
BIN_PATH = bin/

MAIN = RINGO_Project

all:
	$(JAVAC) $(SRC_PATH)*.java -d $(BIN_PATH)

clean:
	rm -f $(BIN_PATH)*.class

run:
	$(JVM) -classpath $(BIN_PATH) $(MAIN)

runa:
	$(JVM) -classpath $(BIN_PATH) Machine 127.0.0.1 5900 6000 7000

tar: clean
	tar cf $(NAME_JAR).tar Makefile README.txt bin/ src/ javadoc/

doc:
	javadoc -encoding utf8 -docencoding utf8 -charset utf8 -d javadoc -author $(SRC_PATH)*.java
