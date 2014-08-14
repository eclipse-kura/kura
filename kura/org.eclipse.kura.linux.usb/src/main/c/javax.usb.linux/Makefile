
# Copyright (c) 1999 - 2001, International Business Machines Corporation.
# All Rights Reserved.
#
# This software is provided and licensed under the terms and conditions
# of the Common Public License:
# http://oss.software.ibm.com/developerworks/opensource/license-cpl.html

# Makefile for Linux Java Usb
#
# Dan Streetman
#

# Object and header dependencies - these are removed by 'make clean'
OBJECTS = \
	JavaxUsbKernel.o \
	JavaxUsbLog.o \
	JavaxUsbTopologyUpdater.o JavaxUsbTopologyListener.o JavaxUsbActive.o \
	JavaxUsbDeviceProxy.o \
	JavaxUsbPipeRequest.o JavaxUsbInterfaceRequest.o \
	JavaxUsbControlRequest.o JavaxUsbBulkRequest.o JavaxUsbInterruptRequest.o JavaxUsbIsochronousRequest.o \
	JavaxUsbError.o
HEADER = com_ibm_jusb_os_linux_JavaxUsb.h

JSR80_CC = gcc
JSR80_CCLD = $(JSR80_CC)

JSR80_INCLUDES = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux $(CINCLUDES)

JSR80_CFLAGS = -Wall -fPIC $(JUSB_FLAGS) $(CFLAGS)
JSR80_SOFLAGS = -shared $(SOFLAGS)

JSR80_COMPILE = $(JSR80_CC) $(JSR80_INCLUDES) $(JSR80_CFLAGS)
JSR80_LINK = $(JSR80_CCLD) $(JSR80_CFLAGS) -o $@

# This is the final shared library
all: libJavaxUsb.so

.SUFFIXES:
.SUFFIXES: .c .o

.c.o:
	$(JSR80_COMPILE) -c $<

# Build the shared library
libJavaxUsb.so: $(OBJECTS)
	@[ -n "${JAVA_HOME}" ] || (echo "";echo "*** Please set your JAVA_HOME variable (I need the JNI headers!) ***";echo "";exit 1)
	@[ -f ${HEADER} ] || (echo "Could not find ${HEADER} header file!";exit 1)
	@rm -f libJavaxUsb.so
	$(JSR80_LINK) $(JSR80_SOFLAGS) $(OBJECTS)

# Cleanup all object and shared files and JAVAH-generated headers (not all headers!!)
clean:
	rm -f libJavaxUsb.so $(OBJECTS) $(HEADER) *.o *.so
