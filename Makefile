JAVAC=javac
JVM = java

SRC_PATH = src/
BIN_PATH = bin/

MAIN = RINGO_Project
NAME_JAR = RINGO_Project

all:
	$(JAVAC) $(SRC_PATH)*.java -d $(BIN_PATH)

clean:
	rm -f $(BIN_PATH)*.class

run:
	$(JVM) -classpath $(BIN_PATH) $(MAIN)

runa:
	$(JVM) -classpath $(BIN_PATH) MachineSA 225.1.2.4 5900 6000 7000

runb:
	$(JVM) -classpath $(BIN_PATH) MachineSA 225.1.2.4 5905 6005 7000

tar: clean
	tar cf $(NAME_JAR).tar Makefile README.txt bin/ src/ javadoc/

doc:
	javadoc -encoding utf8 -docencoding utf8 -charset utf8 -d javadoc -author $(SRC_PATH)*.java

jar:
	echo "Main-Class: $(MAIN)\n" > META-INF/MANIFEST.MF
	jar cvmf META-INF/MANIFEST.MF $(NAME_JAR).jar -C $(BIN_PATH) .

jarsa:
	echo "Main-Class: MachineSA\n" > META-INF/MANIFEST.MF
	jar cvmf META-INF/MANIFEST.MF Machine.jar -C $(BIN_PATH) .

runjar:
	$(JVM) -jar $(NAME_JAR).jar

runjarsa:
	$(JVM) -jar Machine.jar 225.1.2.4 5900 6000 7000