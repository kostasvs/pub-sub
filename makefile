JFLAGS = -g
JC = javac

.SUFFIXES: .java .class

.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
		  Broker.java \
		  ClientWrapper.java \
		  CommandFileHandler.java \
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
	@java Subscriber -i s1 -r 8000 -h 127.0.0.1 -p 9000 -f subscriber1.cmd

pub: classes
	@java Publisher -i p1 -r 8200 -h 127.0.0.1 -p 9000 -f publisher1.cmd
	
sub2: classes
	@java Subscriber -i s2 -r 8001 -h 127.0.0.1 -p 9000 

pub2: classes
	@java Publisher -i p2 -r 8201 -h 127.0.0.1 -p 9000
