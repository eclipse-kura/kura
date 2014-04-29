
# Copyright (c) 1999 - 2001, International Business Machines Corporation.
# All Rights Reserved.
#
# This software is provided and licensed under the terms and conditions
# of the Common Public License:
# http://oss.software.ibm.com/developerworks/opensource/license-cpl.html

# Makefile for Linux Ext Java Usb
#
# Dan Streetman
#

# Object and header dependencies - these are removed by 'make clean'
OBJECTS = \
	LinuxUdev.o
HEADER = org_eclipse_kura_linux_usb_LinuxUdevNative.h

CC = gcc
CCLD = $(CC)

#INCLUDES = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux $(CINCLUDES)
INCLUDES = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux -I../java_home/include/ $(CINCLUDES)

CFLAGS = -Wall -fPIC $(JUSB_FLAGS)
SOFLAGS = -shared

COMPILE = $(CC) $(INCLUDES) $(CFLAGS)
LINK = $(CCLD) $(CFLAGS) -ludev -o $@

# This is the final shared library
all: libEurotechLinuxUdev.so

.SUFFIXES:
.SUFFIXES: .c .o

.c.o:
	$(COMPILE) -c $<

# Build the shared library
libEurotechLinuxUdev.so: $(OBJECTS)
	@[ -n "${JAVA_HOME}" ] || (echo "";echo "*** Please set your JAVA_HOME variable (I need the JNI headers!) ***";echo "";exit 1)
	@[ -f ${HEADER} ] || (echo "Could not find ${HEADER} header file!";exit 1)
	@rm -f libEurotechLinuxUdev.so
	$(LINK) $(SOFLAGS) $(OBJECTS) -ludev

# Cleanup all object and shared files and JAVAH-generated headers (not all headers!!)
clean:
	rm -f libEurotechLinuxUdev.so $(OBJECTS) *.o *.so
#	rm -f libEurotechLinuxUdev.so $(OBJECTS) $(HEADER) *.o *.so
