##
## Makefile for the snarfed.org SnipSnap macros. You'll want to edit
## SNIPSNAPLIBS to point to your SnipSnap installation.
##

## NOTE: this requires snipsnap 0.4.2a!
## Please replace these with your snipsnap installation's directory and primary
## application.
##
SNIPSNAPDIR = /home/ryanb/snipsnap-0.4.2a
LIBS = $(SNIPSNAPDIR)/applications/sandbox/WEB-INF/lib


JAVAC = javac
JAVA = java
JAR = jar
JFLAGS_DEV =
JFLAGS_REL =
JFLAGS = $(JFLAGS_DEV)
CLASSPATH = $(LIBS)/radeox.jar:$(LIBS)/snipsnap-servlets.jar:$(LIBS)/jdom-b8.jar:$(LIBS)/junit.jar:.
METAFILE = META-INF/services/org.radeox.macro.Macro
SRCS = $(shell ls org/snarfed/snipsnap/*.java)
CLASSES = $(SRCS:.java=.class)


default: compile

compile: $(SRCS)
	$(JAVAC) -classpath $(CLASSPATH) $(JFLAGS) $(SRCS)

jar: compile
	$(JAR) cvf $(LIBS)/snarfed.jar $(CLASSES) $(METAFILE) ./README ./LICENSE

test: compile
	$(JAVA) -classpath $(CLASSPATH) junit.textui.TestRunner \
		org.snarfed.snipsnap.AllTests

#SNARFED_DIR = $(shell cd ..; ls -d snarfed*)
#tar:
#	tar czvf $(SNARFED_DIR).tar.gz -C .. --exclude=.svn --no-anchored \
#	$(SNARFED_DIR)

reload:
	cd $(SNIPSNAPDIR); ./run.sh -admin reload sandbox

clean:
	rm -f $(LIBS)/snarfed.jar $(SRCS:.java=.java~) \
	  $(shell ls org/snarfed/snipsnap/*.class)

