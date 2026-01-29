# Makefile
# $Header$
include /home/dev/src/Makefile.common

BINDIR 		= .
DOCSDIR 	= .


DOCFLAGS 	= -version -author -private
SRCS = TestSendScheduleRequest.java

OBJS 		= $(SRCS:%.java=$(BINDIR)/%.class) 
DOCS 		= $(SRCS:%.java=$(DOCSDIR)/%.html)
SCRIPTS		=

top: $(OBJS)
#docs

$(BINDIR)/%.class:%.java
	$(JAVAC) $(JAVAC_OPTIONS) -d $(BINDIR) $<

docs: $(DOCS)

$(DOCS): $(SRCS)
	$(JAVADOC) -d $(DOCSDIR) $(DOCFLAGS) $(SRCS)

