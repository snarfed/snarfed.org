##
## Makefile for the snarfed.org SnipSnap macros. You'll want to edit
## SNIPSNAPLIBS to point to your SnipSnap installation.
##

## NOTE: this requires snipsnap 1.0b1-uttoxeter!
## Please replace these with your snipsnap installation's directory and primary
## application.
##
SNIPSNAPDIR = /home/ryanb/shared/snipsnap-1.0b1-uttoxeter
LIBS = /home/ryanb/applications/_8668_/webapp/WEB-INF/lib

VERSION = 0.2
PKGNAME = snarfed-$(VERSION)

JAVAC = javac
JAVA = java
JAR = jar
JFLAGS_DEV =
JFLAGS_REL =
JFLAGS = $(JFLAGS_DEV)
CLASSPATH = $(LIBS)/radeox.jar:$(LIBS)/snipsnap-utils.jar:$(LIBS)/snipsnap-servlets.jar:$(LIBS)/dom4j.jar:$(SNIPSNAPDIR)/lib/junit.jar:.
METAFILE = META-INF/services/org.radeox.macro.Macro
SRCS = $(shell ls org/snarfed/snipsnap/*.java)
CLASSES = $(SRCS:.java=.class)


default: compile

compile: $(SRCS)
	$(JAVAC) -classpath $(CLASSPATH) $(JFLAGS) $(SRCS)

test: compile
	$(JAVA) -classpath $(CLASSPATH) junit.textui.TestRunner \
		org.snarfed.snipsnap.AllTests

jar: compile
	$(JAR) cf snarfed.jar $(CLASSES) $(METAFILE) ./README ./LICENSE
	if [ ! -e $(LIBS)/snarfed.jar ]; then ln -s $(PWD)/snarfed.jar $(LIBS)/; fi

dist: clean
	ln -s . $(PKGNAME)
	tar cjhf $(PKGNAME).tar.bz2 --exclude $(PKGNAME).tar.bz2 --exclude .svn \
	  --exclude $(PKGNAME)/$(PKGNAME) $(PKGNAME)
	rm $(PKGNAME)

reload:
	cd $(SNIPSNAPDIR); ./run.sh -admin reload sandbox

clean:
	rm -f snarfed.jar $(PKGNAME).tar.bz2 $(shell find . -name \*.class)

