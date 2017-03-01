/*************************************************************************
 * Copyright (c) 1999, 2009 IBM.                                         *
 * All rights reserved. This program and the accompanying materials      *
 * are made available under the terms of the Eclipse Public License v1.0 *
 * which accompanies this distribution, and is available at              *
 * http://www.eclipse.org/legal/epl-v10.html                             *
 *                                                                       *
 * Contributors:                                                         *
 *     IBM - initial API and implementation                              *
 ************************************************************************/
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <time.h>
#include <windows.h>
#include <winbase.h>
#include <winspool.h>
#include <sys/types.h>
#include "org_eclipse_soda_dk_comm_NSParallelPort.h"
#include "NSCommLOG.h"
#define NOOF_ELEMS(s)	((sizeof(s))/(sizeof(s[0])))
#define _PRINTER_
BOOL w32ParallelPort_GetJobs( HANDLE, JOB_INFO_2 **, int *, DWORD * );
/*
 * Function:   w32ParallelPort_closeDeviceNC
 * Purpose:    Close a parallel I/O Device
 * Signature: (Ljava/lang/String;I)I
 * Return:     0 - Fail
 *			   none zero - succeeds
 */
int w32ParallelPort_closeDeviceNC
  (JNIEnv *jenv, jobject jobj, jint fd, jint semId)
{
	HANDLE osHandle = (HANDLE) fd;
	jenv;
#ifdef _PRINTER_
	return ClosePrinter(osHandle);
#else
	return CloseHandle(osHandle);
#endif
}	/* w32ParallelPort_closeDeviceNC */
/*
 * Function:   w32ParallelPort_openDeviceNC
 * Purpose:    Open a parallel I/O Device
 * Signature: (Ljava/lang/String;I)I
 * Return:     0 - Fail
 *			   none zero - device handle
 */
int w32ParallelPort_openDeviceNC(JNIEnv *jenv, jobject jobj, jstring name, jint semId)
{
	char	* portName = NULL;
	int		  fd = -1;
	HANDLE	  osHandle = (HANDLE)fd;
	portName = (CHAR *)(*jenv)->GetStringUTFChars(jenv, name, 0);
	if( portName == NULL )  {
#ifdef DEBUG
		printf( "Can not open port because get NULL name\n" );
#endif
		return (int)osHandle;
	}
#ifdef _PRINTER_
	fd = OpenPrinter( portName,				/* printer or server name	*/
					  &osHandle,			/* printer or server handle	*/
					  NULL );				/* printer defaults			*/
	if(fd == -1) {
		fd = GetLastError();
        iveSerThrowWin(jenv,"parallel port exception: the port isn't valid.\n",fd);
		return 0;
	}
#else
	osHandle =	CreateFile( portName,
							GENERIC_WRITE,
							0,
							0,
							OPEN_EXISTING,
							FILE_ATTRIBUTE_NORMAL | FILE_FLAG_OVERLAPPED,
							0);
	if (INVALID_HANDLE_VALUE == osHandle) {
		fd = GetLastError();
		iveSerThrowWin(jenv,"Error opening file",fd);
		return 0;
	}
#endif
	
	(*jenv)->ReleaseStringUTFChars(jenv, name, portName);
	
	return (int)osHandle;
}  /* w32ParallelPort_openDeviceNC */
/*
 * Function:   w32ParallelPort_isPaperOutNC
 * Purpose:    Check the print paper out status
 * Signature:  (I)Z
 * Return:     TRUE, FALSE
 */
BOOL w32ParallelPort_isPaperOutNC(JNIEnv *jenv, jobject jobj, jint jfd)
{
#ifdef _PRINTER_
	HANDLE		 osHandle = (HANDLE)jfd;
	JOB_INFO_2  *pJobs;
	int			 cJobs;
	DWORD		 dwPrinterStatus;
    /*
     *  Get the state information for the Printer Queue and
     *  the jobs in the Printer Queue.
     */ 
    if (!w32ParallelPort_GetJobs(osHandle, &pJobs, &cJobs, &dwPrinterStatus))
		return FALSE;
    if (dwPrinterStatus & (PRINTER_STATUS_ERROR     |
						   PRINTER_STATUS_PAPER_JAM | 
						   PRINTER_STATUS_PAPER_OUT |
						   PRINTER_STATUS_PAPER_PROBLEM )) {
		free( pJobs );
		return TRUE;
	} else
	return FALSE;
#else
	return FALSE;
#endif
}	/* w32ParallelPort_isPaperOutNC */
/*
 * Function:   w32ParallelPort_isPrinterBusyNC
 * Purpose:    Check the print busy status
 * Signature:  (I)Z
 * Return:     TRUE, FALSE
 */
BOOL w32ParallelPort_isPrinterBusyNC(JNIEnv *jenv, jobject jobj, jint jfd)
{
#ifdef _PRINTER_
	HANDLE		 osHandle = (HANDLE)jfd;
	JOB_INFO_2  *pJobs;
	int			 cJobs;
	DWORD		 dwPrinterStatus;
    /*
     *  Get the state information for the Printer Queue and
     *  the jobs in the Printer Queue.
     */ 
    if (!w32ParallelPort_GetJobs(osHandle, &pJobs, &cJobs, &dwPrinterStatus))
		return FALSE;
    if (dwPrinterStatus & (PRINTER_STATUS_ERROR | 
		PRINTER_STATUS_BUSY )) {
		free( pJobs );
		return TRUE;
	} else
		return FALSE;
#else
	return TFALSE;
#endif
}	/* w32ParallelPort_isPrinterBusyNC */
/*
 * Function:   w32ParallelPort_isPrinterBusyNC
 * Purpose:    Check the print select status
 * Signature:  (I)Z
 * Return:     TRUE, FALSE
 */
BOOL w32ParallelPort_isPrinterSelectedNC(JNIEnv *jenv, jobject jobj, jint jfd)
{
#ifdef _PRINTER_
	HANDLE		 osHandle = (HANDLE)jfd;
	JOB_INFO_2  *pJobs;
	int			 cJobs;
	DWORD		 dwPrinterStatus;
    /*
     *  Get the state information for the Printer Queue and
     *  the jobs in the Printer Queue.
     */ 
    if (!w32ParallelPort_GetJobs(osHandle, &pJobs, &cJobs, &dwPrinterStatus))
		return FALSE;
    if (dwPrinterStatus & PRINTER_STATUS_PROCESSING ) {
		free( pJobs );
		return TRUE;
	} else
		return FALSE;
#else
	return TFALSE;
#endif
}	/* w32ParallelPort_isPrinterSelectedNC */
	
/*
 * Function:   w32ParallelPort_isPrinterBusyNC
 * Purpose:    Check the printer out status
 * Signature:  (I)Z
 * Return:     TRUE, FALSE
 */
BOOL w32ParallelPort_isPrinterTimedOutNC(JNIEnv *jenv, jobject jobj, jint jfd)
{
#ifdef _PRINTER_
	HANDLE		 osHandle = (HANDLE)jfd;
	JOB_INFO_2  *pJobs;
	int			 cJobs;
	DWORD		 dwPrinterStatus;
    /*
     *  Get the state information for the Printer Queue and
     *  the jobs in the Printer Queue.
     */ 
    if (!w32ParallelPort_GetJobs(osHandle, &pJobs, &cJobs, &dwPrinterStatus))
		return FALSE;
    if (dwPrinterStatus & PRINTER_STATUS_PENDING_DELETION ) {
		free( pJobs );
		return TRUE;
	} else
		return FALSE;
#else
	return TFALSE;
#endif
}	/* w32ParallelPort_isPrinterTimedOutNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSParallelPort
 * Method:    isPrinterErrorNC
 * Signature: (I)Z
 * Return:     TRUE, FALSE
 */
BOOL w32ParallelPort_isPrinterErrorNC(JNIEnv *jenv, jobject jobj, jint jfd)
{
#ifdef _PRINTER_
	HANDLE		osHandle = (HANDLE)jfd;
    DWORD       dwPrinterStatus;
    JOB_INFO_2  *pJobs;
    int         cJobs, i;
    /*
     *  Get the state information for the Printer Queue and
     *  the jobs in the Printer Queue.
     */ 
    if (!w32ParallelPort_GetJobs(osHandle, &pJobs, &cJobs, &dwPrinterStatus))
		return FALSE;
    if (dwPrinterStatus & (PRINTER_STATUS_ERROR |
						   PRINTER_STATUS_PAPER_JAM |
						   PRINTER_STATUS_PAPER_OUT |
						   PRINTER_STATUS_PAPER_PROBLEM |
						   PRINTER_STATUS_OUTPUT_BIN_FULL |
						   PRINTER_STATUS_NOT_AVAILABLE |
						   PRINTER_STATUS_NO_TONER |
						   PRINTER_STATUS_OUT_OF_MEMORY |
						   PRINTER_STATUS_OFFLINE |
						   PRINTER_STATUS_DOOR_OPEN)) {
          free( pJobs );
          return TRUE;
     }
     for (i=0; i < cJobs; i++)
     {
        if (pJobs[i].Status & JOB_STATUS_PRINTING)
        {
            if (pJobs[i].Status & (JOB_STATUS_ERROR |
								   JOB_STATUS_OFFLINE |
								   JOB_STATUS_PAPEROUT |
								   JOB_STATUS_BLOCKED_DEVQ)) {
                free( pJobs );
                return TRUE;
            }
        }
     }
    free( pJobs );
    return FALSE;
#endif
}	/* w32ParallelPort_isPrinterErrorNC */
/*
 * Class:      w32ParallelPort_GetJobs
 * Method:     reading the printer / printer job status
 * Signature:  (I)Z
 * Return:     TRUE, FALSE
 */
BOOL w32ParallelPort_GetJobs( HANDLE osHandle, JOB_INFO_2 **ppJobInfo,
							  int *pcJobs, DWORD *pStatus)
{
    DWORD           cByteNeeded,
                    nReturned,
                    cByteUsed;
    JOB_INFO_2      *pJobStorage  = NULL;
    PRINTER_INFO_2  *pPrinterInfo = NULL;
   /* Get the buffer size needed. */ 
    if (!GetPrinter(osHandle, 2, NULL, 0, &cByteNeeded))
    {
        if (GetLastError() != ERROR_INSUFFICIENT_BUFFER)
           return FALSE;
    }
    pPrinterInfo = (PRINTER_INFO_2 *)malloc(cByteNeeded);
    if (!(pPrinterInfo)) return FALSE;
    if (!GetPrinter(osHandle,
				    2,
	 			    (LPSTR)pPrinterInfo,
					cByteNeeded,
					&cByteUsed))
    {
        free(pPrinterInfo);
        pPrinterInfo = NULL;
        return FALSE;
    }
    if (!EnumJobs(osHandle,
				  0,
				  pPrinterInfo->cJobs,
				  2,
				  NULL,
				  0,
				  (LPDWORD)&cByteNeeded,
				  (LPDWORD)&nReturned))
    {
        if (GetLastError() != ERROR_INSUFFICIENT_BUFFER)
        {
            free(pPrinterInfo);
            pPrinterInfo = NULL;
            return FALSE;
        }
    }
    pJobStorage = (JOB_INFO_2 *)malloc(cByteNeeded);
    if (!pJobStorage)
    {
        free(pPrinterInfo);
        pPrinterInfo = NULL;
        return FALSE;
    }
    ZeroMemory(pJobStorage, cByteNeeded);
    if (!EnumJobs(osHandle,
				  0,
				  pPrinterInfo->cJobs,
				  2,
				  (LPBYTE)pJobStorage,
				  cByteNeeded,
				  (LPDWORD)&cByteUsed,
				  (LPDWORD)&nReturned))
    {
         free(pPrinterInfo);
         free(pJobStorage);
         pJobStorage = NULL;
         pPrinterInfo = NULL;
         return FALSE;
    }
    *pcJobs = nReturned;
    *pStatus = pPrinterInfo->Status;
    *ppJobInfo = pJobStorage;
    free(pPrinterInfo);
    return TRUE;
} /* w32ParallelPort_GetJobs() */
