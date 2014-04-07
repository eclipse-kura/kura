JAVAC?=javac
JAVADOC?=javadoc
JAR?=jar
JAVAH?=javah
GCJ?=gcj
CC?=gcc
LD?=gcc
JPPFLAGS+=-C -P
CFLAGS+=-Wall -Os -pedantic -Werror
CSTD?=-std=c99
CSHAREFLAG+=-fpic -fno-stack-protector
GCJJNIFLAG=-fjni
JVERCFLAGS+=-source 1.5
JCFLAGS+=
INCLUDES+=-I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux
JAVADOCFLAGS?=-quiet -author -link http://java.sun.com/j2se/1.4.2/docs/api/

LDVER?=$(shell ld -v | cut -d' ' -f1)
UNAME?=$(shell uname -s)

ifeq ($(LDVER),GNU)
LDSHAREFLAGS+=-fpic -shared
else
LDSHAREFLAGS+=-lc
endif

PREFIX?=/usr/local
JARDIR?=$(PREFIX)/share/java
DOCDIR?=$(PREFIX)/share/doc/libmatthew-java/
LIBDIR?=$(PREFIX)/lib/jni

MATTVER=0.8
DEBUGVER=1.1
UNIXVER=0.5
CGIVER=0.6
IOVER=0.1
HEXVER=0.2

SRC=$(shell find cx -name '*.java' -and -not -name 'Debug.java')

DEBUG?=disable

.NOPARALLEL:
.NO_PARALLEL:
.NOTPARALLEL:

all: unix-$(UNIXVER).jar cgi-$(CGIVER).jar debug-enable-$(DEBUGVER).jar debug-disable-$(DEBUGVER).jar io-$(IOVER).jar hexdump-$(HEXVER).jar libcgi-java.so libunix-java.so

classes: .classes 
.classes: $(SRC) 
	mkdir -p classes
	$(MAKE) .$(DEBUG)debug
	$(JAVAC) $(JVERCFLAGS) $(JCFLAGS) -d classes -cp classes $^
	touch .classes
clean:
	rm -rf classes doc
	rm -f .classes .enabledebug .disabledebug *.o *.h *.so *.tar.gz *.jar *.cgi Manifest
	rm -rf libmatthew-java-$(MATTVER)

cgi-$(CGIVER).jar: .classes
	(cd classes; $(JAR) cf ../$@ cx/ath/matthew/cgi/*class)
io-$(IOVER).jar: .classes
	(cd classes; $(JAR) cf ../$@ cx/ath/matthew/io/*class)
unix-$(UNIXVER).jar: .classes
ifeq ($(DEBUG),enable)
	echo "Class-Path: $(JARDIR)/debug-$(DEBUG).jar" > Manifest
else
	echo "Class-Path: " > Manifest
endif
	(cd classes; $(JAR) cfm ../$@ ../Manifest cx/ath/matthew/unix/*class)

hexdump-$(HEXVER).jar: .classes
	(cd classes; $(JAR) cf ../$@ cx/ath/matthew/utils/Hexdump.class)

%.o: %.c %.h
	$(CC) $(CFLAGS) $(CSTD) $(CSHAREFLAG) $(INCLUDES) -c -o $@ $<
lib%.so: %.o
	$(CC) $(LDFLAGS) $(LDSHAREFLAGS) -o $@ $<
unix-java.h: .classes
	$(JAVAH) -classpath classes -o $@ cx.ath.matthew.unix.UnixServerSocket cx.ath.matthew.unix.UnixSocket cx.ath.matthew.unix.USInputStream cx.ath.matthew.unix.USOutputStream
cgi-java.h: .classes
	$(JAVAH) -classpath classes -o $@ cx.ath.matthew.cgi.CGI

test.cgi: cgi-$(CGIVER).jar libcgi-java.so
	$(GCJ) $(GCJFLAGS) $(GCJJNIFLAG) -L. -lcgi-java -o test.cgi --main=cx.ath.matthew.cgi.testcgi cgi-$(CGIVER).jar
	
libmatthew-java-$(MATTVER).tar.gz: Makefile cx cgi-java.c unix-java.c README INSTALL COPYING changelog
	mkdir -p libmatthew-java-$(MATTVER)
	cp -a $^ libmatthew-java-$(MATTVER)
	tar zcf $@ libmatthew-java-$(MATTVER)

debug-enable-$(DEBUGVER).jar: cx/ath/matthew/debug/Debug.jpp
	make .enabledebug
	echo "Class-Path: $(JARDIR)/hexdump.jar" > Manifest
	(cd classes;jar cfm ../$@ ../Manifest cx/ath/matthew/debug/*.class)
debug-disable-$(DEBUGVER).jar: cx/ath/matthew/debug/Debug.jpp
	make .disabledebug
	echo "Class-Path: $(JARDIR)/hexdump.jar" > Manifest
	(cd classes;jar cfm ../$@ ../Manifest cx/ath/matthew/debug/*.class)
.enabledebug: cx/ath/matthew/debug/Debug.jpp 
	mkdir -p classes
	cpp $(PPFLAGS) $(JPPFLAGS) -DDEBUGSETTING=true < cx/ath/matthew/debug/Debug.jpp > cx/ath/matthew/debug/Debug.java
	$(JAVAC) $(JVERCFLAGS) $(JCFLAGS) -cp classes -d classes cx/ath/matthew/debug/Debug.java cx/ath/matthew/utils/Hexdump.java
	rm -f .disabledebug
	touch .enabledebug
.disabledebug: cx/ath/matthew/debug/Debug.jpp 
	mkdir -p classes
	cpp $(PPFLAGS) $(JPPFLAGS) -DDEBUGSETTING=false < cx/ath/matthew/debug/Debug.jpp > cx/ath/matthew/debug/Debug.java
	$(JAVAC) $(JVERCFLAGS) $(JCFLAGS) -cp classes -d classes cx/ath/matthew/debug/Debug.java cx/ath/matthew/utils/Hexdump.java
	rm -f .enabledebug
	touch .disabledebug
cx/ath/matthew/debug/Debug.java: .disabledebug
doc/index.html: 
	$(JAVADOC) $(JAVADOCFLAGS) -d doc/ cx/ath/matthew/debug/Debug.java $(SRC)

doc: doc/index.html

install-doc: doc/index.html
	install -d $(DESTDIR)$(DOCDIR)
	cp -a doc $(DESTDIR)$(DOCDIR)/api

install-native: libcgi-java.so libunix-java.so 
	install -d $(DESTDIR)$(LIBDIR) 
	install libcgi-java.so $(DESTDIR)$(LIBDIR)
	install libunix-java.so $(DESTDIR)$(LIBDIR)

install-jar: unix-$(UNIXVER).jar cgi-$(CGIVER).jar debug-enable-$(DEBUGVER).jar debug-disable-$(DEBUGVER).jar io-$(IOVER).jar hexdump-$(HEXVER).jar
	install -d $(DESTDIR)$(JARDIR)
	install -m 644 debug-enable-$(DEBUGVER).jar $(DESTDIR)$(JARDIR)
	install -m 644 debug-disable-$(DEBUGVER).jar $(DESTDIR)$(JARDIR)
	install -m 644 unix-$(UNIXVER).jar $(DESTDIR)$(JARDIR)
	install -m 644 cgi-$(CGIVER).jar $(DESTDIR)$(JARDIR)
	install -m 644 io-$(IOVER).jar $(DESTDIR)$(JARDIR)
	install -m 644 hexdump-$(HEXVER).jar $(DESTDIR)$(JARDIR)
	ln -sf debug-disable-$(DEBUGVER).jar $(DESTDIR)$(JARDIR)/debug-disable.jar
	ln -sf debug-enable-$(DEBUGVER).jar $(DESTDIR)$(JARDIR)/debug-enable.jar
	ln -sf unix-$(UNIXVER).jar $(DESTDIR)$(JARDIR)/unix.jar
	ln -sf io-$(IOVER).jar $(DESTDIR)$(JARDIR)/io.jar
	ln -sf cgi-$(CGIVER).jar $(DESTDIR)$(JARDIR)/cgi.jar
	ln -sf hexdump-$(HEXVER).jar $(DESTDIR)$(JARDIR)/hexdump.jar

install: install-native install-jar
