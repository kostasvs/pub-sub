JFLAGS = -g
JC = javac

.SUFFIXES: .java .class

.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
		  Broker.java \
		  ClientWrapper.java \
		  Params.java \
		  Publisher.java \
		  ServerClientHandler.java \
		  ServerWrapper.java \
		  Subscriber.java \
		  UserInput.java \
		  Utils.java

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
	
broker: classes
	@java Broker -p 9001 -s 9000
	
sub: classes
	@java Subscriber -i s1 -r 8000 -h 127.0.0.1 -p 9000