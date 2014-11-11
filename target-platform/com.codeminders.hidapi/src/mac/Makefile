###########################################
# Simple Makefile for HIDAPI test program
#
# Alan Ott
# Signal 11 Software
# 2010-07-03
###########################################

#ARCHFLAGS=-m32

JNIOBJS=HIDManager.o HIDDeviceInfo.o HIDDevice.o hid-java.o
JNIINCLUDES=-I.. -I../jni-impl -I/System/Library/Frameworks/JavaVM.framework/Headers 
JNILIBS=-l iconv
JNISHAREDLIB=libhidapi-jni.jnilib
JNISHAREDLIBVER=1.0

CC=gcc
CXX=g++
COBJS=hid.o
CPPOBJS=../hidtest/hidtest.o
OBJS=$(COBJS) $(CPPOBJS) $(JNIOBJS)
CFLAGS+=$(ARCHFLAGS) -I../hidapi -g -c $(JNIINCLUDES)
LIBS=-framework IOKit -framework CoreFoundation $(JNILIBS)

all: hidtest $(JNISHAREDLIB)

$(JNISHAREDLIB): $(OBJS)
	$(CXX) $(ARCHFLAGS) -dynamiclib -current_version $(JNISHAREDLIBVER) $(COBJS) $(JNIOBJS) $(LIBS) -o $(JNISHAREDLIB)

hidtest: $(OBJS)
	$(CXX) $(ARCHFLAGS) -g $^ $(LIBS) -o hidtest

%.o: ../jni-impl/%.cpp
	$(CXX) $(CFLAGS) $< -o $@

$(COBJS): %.o: %.c
	$(CC) $(CFLAGS) $< -o $@

$(CPPOBJS): %.o: %.cpp
	$(CXX) $(CFLAGS) $< -o $@

clean:
	rm -f *.o hidtest $(CPPOBJS) $(JNISHAREDLIB)

.PHONY: clean
