#########################################################################
# Copyright (c) 2001, 2009 IBM.                                         #
# All rights reserved. This program and the accompanying materials      #
# are made available under the terms of the Eclipse Public License v1.0 #
# which accompanies this distribution, and is available at              #
# http://www.eclipse.org/legal/epl-v10.html                             #
#                                                                       #
# Contributors:                                                         #
#     IBM - initial API and implementation                              #
#########################################################################
DLLNAME=Release/win32/x86/dkcomm22.dll
all: 
	@echo "Usage: make platform"    ;
	@echo "	Where platform = linux | qnx" ;
linux:
	$(MAKE) -f makefile.linux
qnx:
	$(MAKE) -f makefile.qnx
	
clean:
	rm -f *.o
	rm -f *.obj
	rm -f $(DLLNAME)
		
	
	
