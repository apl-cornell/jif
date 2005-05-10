SOURCE = ../../../../../..
PACKAGE = jif/principal
SUBDIRS =
BUILDPATH = ../..

include $(SOURCE)/Rules.mk

SOURCES = \
	Alice.java \
	Bob.java \
	Chuck.java \
	Dolores.java \
	Ethel.java \
	root.java \
	client.java \
	server.java \
	preparer.java \
	Etrade.java \
	Citibank.java  \
	broker.java \
	bank.java \
	irs.java \
	cra1.java \
	cra2.java

CLASSES = $(patsubst %.java, $(PACKAGEPATH)/%.class, $(SOURCES))

MANIFEST = Makefile $(SOURCES)

JAVAC_PATHS = -classpath $(CLASSPATH) -sourcepath $(BUILDPATH)

local: 
	$(BIN)/localusers

all: $(CLASSES) 

clean clobber: cleanclasses

DEMOS = \
	Alice.java \
	Bob.java \
	Chuck.java \
	Dolores.java \
	Ethel.java \

