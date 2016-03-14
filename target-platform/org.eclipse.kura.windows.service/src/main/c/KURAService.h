/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/

#ifndef _kuraservice_h_
#define _kuraservice_h_

#include <windows.h>
#include <tchar.h>

#define SERVICE_NAME  _T("KURA Service")    

// Defined in main.c, gets set by a SERVICE_CONTROL_STOP request and is used to indicate to the service loop
// that the service is stopping and the actual KURA process should not be restarted when it dies.
extern BOOL g_bTerminate;

// Defined in utils.c, handles for the read and write ends of the two pipes used for STDIO of the child process 
extern HANDLE g_hOUTRead, g_hOUTWrite, g_hINRead, g_hINWrite;

// Defined in utils.c, holds the current sate of the service.
extern SERVICE_STATUS g_ServiceStatus;

// Defined in utils.c, an identifier that allows reporting of the status of this service to the service manager 
extern SERVICE_STATUS_HANDLE g_StatusHandle;

// Defined in utils.c, this function notifies the service manager of a change in status of the service
extern void UpdateStatus(DWORD dwStatus, DWORD dwAccepts, DWORD dwCheck, DWORD dwError);

// Defined in service.c, this function creates a child process that actually runs the KURA service 
extern void RunService(TCHAR *pszCommand);

// Defined in utils.c, this function is used to create a thread to absorb the STDOUT data from the child process
extern DWORD WINAPI STDOUT_Thread(void *lpParameter);

// Defined in utils.c, this function creates two pipes which are used for the STDIN & STDOUT of the child process
extern BOOL CreatePipes(void);

// Defined in service.c, provides the callback to handle control messages from the service manager
extern void WINAPI ServiceCtrlHandler(DWORD CtrlCode);

#endif
