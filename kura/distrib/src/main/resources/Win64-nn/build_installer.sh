#!/bin/bash
#
# This script is used to build the final installer. It uses the NSIS tool makensis to compile the Kura_x??.nsi script into a
# setup.exe style installer. The ANT process that runs it will ensure that it's in the correct directory first

#==============================================================================================================================
# First check to see if the nsis tools are installed, if so chage to the InstallWin directory and run the build. If not just
# print a waring and exit.

if [ -x "$(command -v makensis)" ]; then
	makensis Kura_x64.nsi
else
	echo WARNING: The NSIS tools are not installed the Windows installer program will not be built.
fi