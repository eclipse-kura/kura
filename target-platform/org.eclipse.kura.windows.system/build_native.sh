#!/bin/bash

#*******************************************************************************
# Copyright (c) 2011, 2016 Eurotech and/or its affiliates
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#     Eurotech
#*******************************************************************************

# This script is used to build the KURA Native DLLs with the OS specific call on Windows based systems. It relies on
# mingw64 to do the build. If mingw64 is not installed it skips the build without error so that the pre built binary will be used
#
# It's called with one of two arguments 'clean' in which case the object files are deleted, or 'build' in which case the files are
# rebuilt. Take care here to make sure that the required build tools are installed, i686-w64-mingw32-gcc for the 32 bit version
# and x86_64-w64-mingw32-gcc for the 64 bit version. These should all be installed with the mingw64 package.

#==============================================================================================================================
# First check to see what make we have, if it's Windows it will probably be mingw32-make, if it's Linux it's probably just make
# either way set the MAKECMD variable accordingly. If we can't find either issue an warning message. The empty MAKECMD string 
# will be caught later causing the pre built binaries to be copied to the output directory.
#
if [ -x "$(command -v mingw32-make)" ] || [ -x "$(command -v make)" ]; then
	if [ -x "$(command -v make)" ]; then
		MAKECMD="make"
	else
		MAKECMD="mingw32-make"
	fi
else
	echo "  WARNING: No viable 'make' command installed. KURA Windows Native will not be rebuilt"
fi

#===============================================================================================================================
# If the argument is 'clean' just work through all the files in the Objs directory and delete them.
#
if [ "$1" == "clean" ]; then
	for f in src/main/c/Objs/x86/*; do
		if [ -f "$f" ]; then
			echo Deleting $f
			rm $f
		fi
	done
	for f in src/main/c/Objs/x64/*; do
		if [ -f "$f" ]; then
			echo Deleting $f
			rm $f
		fi
	done
fi

#===============================================================================================================================
# If the argument is 'build' change to the source directory, create the Objs and Release directories if required, then check to
# see if we have viable 32 bit compiler if so call make with the 32 bit target. Next check for 64 bit compiler and run make with
# the 64 bit target. If the compilers are not found just issue a warning and continue without doing anything. This will leave the
# pre built binaries in the Release directory to be used in the final installer. This should mean that you get a working libs
# even if you don't have mingw installed
#
if [ "$1" == "build" ]; then
		
		cd src/main/c
		if [ ! -d Objs/x86 ]; then
			mkdir -p Objs/x86
		fi

		if [ ! -d Objs/x64 ]; then
			mkdir -p Objs/x64
		fi

		if [ ! -d Release/Win32/x86 ]; then
			mkdir -p Release/Win32/x86
		fi

		if [ ! -d Release/Win32/x64 ]; then
			mkdir -p Release/Win32/x64
		fi

		if [ -x "$(command -v i686-w64-mingw32-gcc)" ] && [ -n "$MAKECMD" ]; then
			$MAKECMD -f makefile.mingw KuraNativeWin32
		else
			cp ../bin/Win32/x86/* Release/Win32/x86
			echo "  WARNING: No viable make or 32 bit 'mingw' compiler installed. 32 bit KURA Windows Native will not be rebuilt"
		fi

		if [ -x "$(command -v x86_64-w64-mingw32-gcc)" ] && [ -n "$MAKECMD" ]; then
			$MAKECMD -f makefile.mingw KuraNativeWin64
		else
			cp ../bin/Win32/x64/* Release/Win32/x64
			echo "  WARNING: No viable make or 64 bit 'mingw' compiler installed. 64 bit KURA Windows Native will not be rebuilt"
		fi
fi
