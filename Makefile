###
## Makefile for the snarfed.org SnipSnap macros. You'll want to edit
## SNIPSNAPDIR and LIBS to point to your SnipSnap installation and application.
##
## NOTE: this requires snipsnap 1.0b1-uttoxeter!
##
SNIPSNAPDIR = $(HOME)/shared/snipsnap-1.0b1-uttoxeter
LIBS = $(HOME)/applications/_8668_/webapp/WEB-INF/lib

VERSION = 0.3
PKGNAME = snarfed-$(VERSION)

# note that gcc's javac can build this package, but gcc's java classloader has
# trouble with it. you can use gcc's javac with another j2re's java, or you can
# use javac and java from sun's j2sdk (bleh).
JAVAC = javac
JAVA = java    # /etc/alternatives/java  (won't work!)
JAR = jar

JFLAGS_DEV =
JFLAGS_REL =
JFLAGS = $(JFLAGS_DEV)
CLASSPATH = $(LIBS)/radeox.jar:$(LIBS)/snipsnap-utils.jar:$(LIBS)/snipsnap-servlets.jar:$(LIBS)/picocontainer-1.0.jar:$(LIBS)/nanocontainer-1.0.jar:$(LIBS)/nanning.jar:$(LIBS)/dom4j.jar:$(SNIPSNAPDIR)/lib/junit.jar:/usr/lib/jre/lib/rt.jar
METAFILE = META-INF/services/org.radeox.macro.Macro
SRCS = $(shell ls org/snarfed/snipsnap/*.java)


default: compile

compile: $(SRCS)
	$(JAVAC) -classpath $(CLASSPATH) $(JFLAGS) $(SRCS)

test: compile
	$(JAVA) -classpath $(CLASSPATH):. junit.textui.TestRunner \
		org.snarfed.snipsnap.AllTests

jar: compile
	$(JAR) cf snarfed.jar org/snarfed/snipsnap/ $(METAFILE) ./README ./LICENSE
	# all on one line because make resets pwd between lines
	if [ ! -e $(LIBS)/snarfed.jar ]; then ln -s $(PWD)/snarfed.jar $(LIBS)/; fi

dist: clean
	ln -s . $(PKGNAME)
	tar cjhf $(PKGNAME).tar.bz2 --exclude $(PKGNAME).tar.bz2 --exclude .svn \
	  --exclude $(PKGNAME)/$(PKGNAME) $(PKGNAME)
	rm $(PKGNAME)

reload:
	# all on one line because make resets pwd between lines
	cd $(SNIPSNAPDIR); ./run.sh -admin shutdown; ./run.sh


clean:
	rm -f snarfed.jar $(PKGNAME).tar.bz2 $(shell find . -name \*.class)

