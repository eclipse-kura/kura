/*******************************************************************************
 * Copyright (c) 2016 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/

#ifndef _kuralauncher_h_
#define _kuralauncher_h_

#include <windows.h>
#include <tchar.h>

// Defined in utils.c, handles for the read and write ends of the two pipes used for STDIO of the child process 
extern HANDLE g_hOUTRead, g_hOUTWrite, g_hINRead, g_hINWrite;

// Defined in utils.c, this function is used to create a thread to absorb the STDOUT data from the child process
extern DWORD WINAPI STDOUT_Thread(void *lpParameter);

// Defined in utils.c, this function creates two pipes which are used for the STDIN & STDOUT of the child process
extern BOOL CreatePipes(void);

#endif
