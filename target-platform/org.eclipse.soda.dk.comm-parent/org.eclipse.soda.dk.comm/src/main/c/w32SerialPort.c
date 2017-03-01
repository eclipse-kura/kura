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
#ifndef _WIN32_WCE
#include <sys/types.h>
#endif //_WIN32_WCE
#include "org_eclipse_soda_dk_comm_NSSerialPort.h"
#include "NSCommLOG.h"
#if 0 //t.j
#ifdef _POSIX_SEMAPHORES
#include <semaphore.h>
#include "SysVStyleSemaphore.h"
#else 
#include <sys/ipc.h> 
#include <sys/sem.h> 
#endif
#define NOOF_ELEMS(s)	((sizeof(s))/(sizeof(s[0])))
#ifndef _POSIX_SEMAPHORES
static struct sembuf	dev_test[] = {
		{ 0, 0, IPC_NOWAIT }	/* test to see if it is free */
};
static struct sembuf	dev_lock[] = {
		{ 0, 0, 0 },	/* wait for the semaphore to be free */
		{ 0, 1, SEM_UNDO }	/* lock it */
};
static struct sembuf	dev_unlock[] = {
		// { 0, -1, (IPC_NOWAIT | SEM_UNDO) }	/* unlock it */
		{ 0, -1,  0  }   	/* wait til unlock it */
};
#endif
#endif //t.j
#define assert(s)  if (!s) {printf("\n\n%d asserted!\n\n", __LINE__); \
				 return(-1);}
/*------------------------------------------------------------------
 * constants
 *------------------------------------------------------------------*/
#define W32COMM_PARITY_NONE       0
#define W32COMM_PARITY_ODD        1
#define W32COMM_PARITY_EVEN       2
#define W32COMM_PARITY_MARK       3
#define W32COMM_PARITY_SPACE      4
#define W32COMM_STOPBITS_1_5      0
#define W32COMM_STOPBITS_1        1
#define W32COMM_STOPBITS_2        2
#define W32COMM_DATABITS_5        5
#define W32COMM_DATABITS_6        6
#define W32COMM_DATABITS_7        7
#define W32COMM_DATABITS_8        8
#define assertexc(s) if (!s) {printf("\n\n%d asserted!\n\n", __LINE__); \
                            return(-1);}
jint w32getfd(JNIEnv *jenv, jobject jobj)
{
    jclass        jc;
    jfieldID      jf;
    jint          fd = -1;
    // Get the file descriptor.
    jc = (*jenv)->GetObjectClass(jenv, jobj);
    assertexc(jc);
    jf = (*jenv)->GetFieldID(jenv, jc, "fd", "I");
    assertexc(jf);
    return (*jenv)->GetIntField(jenv, jobj, jf);
}  /* w32getfd() */
/*
 * Function:   w32SerialPort_closeDeviceNC
 * Purpose:    Close a I/O Device
 * Signature: (Ljava/lang/String;I)I
 * Return:     0 - Fail
 *			   none zero - succeeds
 */
int w32SerialPort_closeDeviceNC
  (JNIEnv *env, jobject jobj, jint fd, jint semId)
{
	HANDLE osHandle = (HANDLE) fd;
	env;
	return CloseHandle(osHandle);
}	/* w32SerialPort_closeDeviceNC */
/*
 * Function:   w32SerialPort_openDeviceNC
 * Purpose:    open a I/O Device
 * Signature: (Ljava/lang/String;I)I
 * Return:     0 - Fail
 *			   none zero - succeeds	
 */
int w32SerialPort_openDeviceNC
  (JNIEnv *jenv, jobject jobj, jstring name, jint semId)
{
#if _WIN32_WCE>=400
	//TCHAR		appends[2] = L":";
	//const unsigned short *portName = NULL;
	//const unsigned short *portName_wince = NULL;
	LPTSTR	portName = NULL;
	LPTSTR	portName_wince = NULL;
	LPTSTR	appends = TEXT(":");
#else
	const char *portName = NULL;
#endif
	int			fd = -1;
	HANDLE      osHandle;
#ifdef WINCE
	portName = (*jenv)->GetStringChars(jenv, name, 0);
#else
	portName = (*jenv)->GetStringUTFChars(jenv, name, 0);
#endif //_WIN32_WCE
	if( portName == NULL )
	{
#ifdef DEBUG
//		LOG(("Can not open port because get NULL name\n"));
		printf( "Can not open port because get NULL name\n" );
	    fflush( stdout );
#endif
		printf("portName is NULL\n");
		return fd;
	}
#if _WIN32_WCE>=400
	lstrcat(portName, appends);
	// TEXT("COM1:")
	
	osHandle =	CreateFile(
				portName,
				GENERIC_READ | GENERIC_WRITE,
				0,
				NULL,
				OPEN_EXISTING,
				0,
				NULL);
#else
	osHandle =	CreateFile(
				portName,
				GENERIC_READ | GENERIC_WRITE,
				0,
				0,
				OPEN_EXISTING,
				FILE_ATTRIBUTE_NORMAL | FILE_FLAG_OVERLAPPED,
				0);
#endif //_WIN32_WCE
	
	if (INVALID_HANDLE_VALUE == osHandle) {
		fd = GetLastError();
		iveSerThrowWin(jenv,"Error opening port", fd);
		return 0;
	}
#if _WIN32_WCE>=400
	(*jenv)->ReleaseStringChars(jenv, name, portName);
#else
	(*jenv)->ReleaseStringUTFChars(jenv, name, portName);
#endif //_WIN32_WCE
#ifdef DEBUG
//		LOG(("w32SerialPort_openDeviceNC(%s)",portName));
		printf( "w32SerialPort_openDeviceNC(%s) osHandle: %d\n", portName, osHandle );
	    fflush( stdout );
#endif
	return (jint) osHandle;
}  /* w32SerialPort_openDeviceNC */
/*
 * Function:   w32SerialPort_setFlowControlModeNC
 * Purpose:    setup the comm port flow control
 * Signature: (Ljava/lang/String;I)I
 * Return:     0 - Fail
 *			   none zero - succeeds	
 */
int w32SerialPort_setFlowControlModeNC(JNIEnv *jenv, jobject jobj, jint fd, jint fc )
{
    HANDLE  osHandle = (HANDLE) fd;
    DWORD   lastRc;
    DCB     dcb;
    BOOL    success;
	(void)memset(&dcb, 0, sizeof(DCB));
    success = GetCommState(osHandle,&dcb);
    if (!success) {
        lastRc = GetLastError();
        iveSerThrowWin(jenv,"GetCommState() failed",lastRc);
        return success;
    }
	if (fc == 0) {			// None for both hardware and software
		 /* Do nothing, as all the flow control modes are already turned off now. */
		dcb.fOutxCtsFlow      = FALSE; //(config->hardwareFlowControl != 0);
	    dcb.fOutxDsrFlow      = FALSE; 
		dcb.fTXContinueOnXoff = FALSE;
	    dcb.fOutX             = FALSE; //(config->softwareFlowControl != 0);
		dcb.fInX              = FALSE; //(config->softwareFlowControl != 0);
	} else {
		if (fc & 1) {		/* hardware flow control RTSCTS_IN */
			dcb.fOutxCtsFlow  = TRUE; 
			dcb.fRtsControl   = RTS_CONTROL_ENABLE;
		 }
		 if (fc & 2) {		/* hardware flow control RTSCTS_OUT */
			dcb.fOutxCtsFlow  = TRUE; 
			dcb.fRtsControl   = RTS_CONTROL_ENABLE;
		 }
		 if (fc & 4) {		/* software flow control XONXOFF_IN */
			dcb.fInX = TRUE;
		 }
		 if (fc & 8) {		/* software flow control XONXOFF_OUT */
			dcb.fOutX  = TRUE;
		 } 
	}
	success = SetCommState(osHandle,&dcb);
	if (!success) {
		lastRc = GetLastError();
		iveSerThrowWin(jenv,"SetCommState() failed",lastRc);
		printf("w32SerialPort_setDTRNC() failed to set the DCB: %d\n", success);
	}
	return success;
}  /* w32SerialPort_setFlowControlModeNC */
/*
 * Function:   w32SerialPort_getFlowControlModeNC
 * Purpose:    get the current comm port flow control settings
 * Signature: (Ljava/lang/String;I)I
 * Return:     0 - Fail
 *			   none zero - succeeds	
 */
int w32SerialPort_getFlowControlModeNC(JNIEnv *jenv, jobject jobj, jint fd )
{
    HANDLE  osHandle = (HANDLE) fd;
    DWORD   lastRc;
    DCB     dcb;
    int     ret = -1;
	(void)memset(&dcb, 0, sizeof(DCB));
    ret = GetCommState(osHandle,&dcb);
    if (!ret) {
        lastRc = GetLastError();
        iveSerThrowWin(jenv,"GetCommState() failed",lastRc);
        return ret;
    }
  // Determine the flow control.
  if ( !dcb.fOutxCtsFlow && !dcb.fOutxDsrFlow && !dcb.fTXContinueOnXoff && !dcb.fOutX && !dcb.fInX )
     ret = 0;				/* None for both hardware and software flow control */
  else {
     ret = 0;
     if ( dcb.fOutxCtsFlow && (dcb.fRtsControl==RTS_CONTROL_ENABLE) )	/* hardware flow control RTSCTS_IN */
        ret |= 1;
     if ( dcb.fOutxCtsFlow && (dcb.fRtsControl==RTS_CONTROL_ENABLE) )	/* hardware flow control RTSCTS_IN */
        ret |= 2;
     if (dcb.fInX  & TRUE)		/* software flow control XONXOFF_IN */
        ret |= 4;
     if (dcb.fOutX & TRUE)		/* software flow control XONXOFF_OUT */
        ret |= 8;
  }
  
  return ret;
} /* w32SerialPort_getFlowControlModeNC */
/*
 * Function:   w32SerialPort_getBaudRateNC
 * Purpose:    get the current comm port baud rate settings
 * Signature: (Ljava/lang/String;I)I
 * Return:     0 - Fail
 *			   none zero - succeeds	
 */
int w32SerialPort_getBaudRateNC(JNIEnv *jenv, jobject jobj, jint fd)
{
    HANDLE  osHandle = (HANDLE) fd;
    DWORD   lastRc;
    DCB     dcb;
    BOOL    success;
	(void)memset(&dcb, 0, sizeof(DCB));
    success = GetCommState(osHandle,&dcb);
    if (!success) {
        lastRc = GetLastError();
        iveSerThrowWin(jenv,"GetCommState() failed",lastRc);
        return success;
    }
#ifdef DEBUG
	printf( "win32SerialPort_getBaudRateNC(), baud rate: %d\n", dcb.BaudRate );
	fflush( stdout ); 
#endif
	return dcb.BaudRate;
}  /* w32SerialPort_getBaudRateNC() */
/*
 * Function:   w32SerialPort_getDataBitsNC
 * Purpose:    get the current comm port data bits settings
 * Signature: (Ljava/lang/String;I)I
 * Return:     0 - Fail
 *			   none zero - succeeds	
 */
int w32SerialPort_getDataBitsNC(JNIEnv *jenv, jobject jobj, jint fd)
{
    HANDLE  osHandle = (HANDLE) fd;
    DWORD   lastRc;
    DCB     dcb;
    BOOL    success;
	(void)memset(&dcb, 0, sizeof(DCB));
    success = GetCommState(osHandle,&dcb);
    if (!success) {
        lastRc = GetLastError();
        iveSerThrowWin(jenv,"GetCommState() failed",lastRc);
        return success;
    }
#ifdef DEBUG
	printf( "w32SerialPort_getDataBitsNC(), baud rate: %d\n", dcb.ByteSize );
	fflush( stdout ); 
#endif
	return dcb.ByteSize;
}  /* w32SerialPort_getDataBitsNC() */
   
/*
 * Function:   w32SerialPort_getStopBitsNC
 * Purpose:    get the current comm port stop bits settings
 * Signature: (Ljava/lang/String;I)I
 * Return:     0 - Fail
 *			   none zero - succeeds	
 */
int w32SerialPort_getStopBitsNC(JNIEnv *jenv, jobject jobj, jint fd)
{
    HANDLE  osHandle = (HANDLE) fd;
    DCB     dcb;
    DWORD   lastRc;
    BOOL    success;
    BYTE    stopBits;
	(void)memset(&dcb, 0, sizeof(DCB));
    success = GetCommState(osHandle,&dcb);
    if (!success) {
        lastRc = GetLastError();
        iveSerThrowWin(jenv,"GetCommState() failed",lastRc);
        return success;
    }
	
    switch (dcb.StopBits) {
		case ONE5STOPBITS:	stopBits = W32COMM_STOPBITS_1_5; break;
        case TWOSTOPBITS:	stopBits = W32COMM_STOPBITS_2;   break;
        case ONESTOPBIT:   
        default:            stopBits = W32COMM_STOPBITS_1;   break;
    }
	return (jint) stopBits;
}  /* w32SerialPort_getStopBitsNC() */
/*
 * Function:   w32SerialPort_getParityNC
 * Purpose:    get the current comm port parity bits settings
 * Signature: (Ljava/lang/String;I)I
 * Return:     0 - Fail
 *			   none zero - succeeds	
 */
int w32SerialPort_getParityNC(JNIEnv *jenv, jobject jobj, jint fd)
{
    HANDLE  osHandle = (HANDLE) fd;
    DCB     dcb;
    DWORD   lastRc;
    BOOL    success;
    BYTE    parity;
	(void)memset(&dcb, 0, sizeof(DCB));
    success = GetCommState(osHandle,&dcb);
    if (!success) {
        lastRc = GetLastError();
        iveSerThrowWin(jenv,"GetCommState() failed",lastRc);
        return success;
    }
	
	if ( dcb.fParity == TRUE )
	{
	    switch (dcb.Parity) {
			case EVENPARITY:   parity = W32COMM_PARITY_EVEN;  break;
			case ODDPARITY:    parity = W32COMM_PARITY_ODD;   break;
			case MARKPARITY:   parity = W32COMM_PARITY_MARK;  break;
			case SPACEPARITY:  parity = W32COMM_PARITY_SPACE; break;
			case NOPARITY:	   
			default:           parity = W32COMM_PARITY_NONE;  break;
    
		}
	}
#ifdef DEBUG
	printf( "win32SerialPort_getParityNC():  parity = %d\n", parity); 
#endif /* DEBUG */
  
	return (jint) parity;
} /* w32SerialPort_getParityNC() */
/*
 * Function:   w32SerialPort_setDTRNC
 * Purpose:    setup the comm port DTR bit settings
 * Signature: (Ljava/lang/String;I)I
 * Return:     none	
 */
void w32SerialPort_setDTRNC(JNIEnv *jenv, jobject jobj, jboolean bool)
{
    DCB     dcb;
    DWORD   lastRc;
    BOOL    success;
    HANDLE  osHandle = (HANDLE) w32getfd(jenv, jobj);
	(void)memset(&dcb, 0, sizeof(DCB));
    success = GetCommState(osHandle,&dcb);
    if (!success) {
        lastRc = GetLastError();
        iveSerThrowWin(jenv,"GetCommState() failed",lastRc);
		printf("w32SerialPort_setDTRNC() failed to get the DCB: %d\n", success);
    }
	dcb.fDtrControl = DTR_CONTROL_ENABLE;
    success = SetCommState(osHandle,&dcb);
    if (!success) {
        lastRc = GetLastError();
        iveSerThrowWin(jenv,"SetCommState() failed",lastRc);
		printf("w32SerialPort_setDTRNC() failed to set the DCB: %d\n", success);
    }
}  /* w32SerialPort_setDTRNC() */
/*
 * Function:   w32SerialPort_isDTRNC
 * Purpose:    checking the current comm port DTR bit settings
 * Signature: (Ljava/lang/String;I)I
 * Return:     0 - Fail
 *			   none zero - succeeds	
 */
BOOL w32SerialPort_isDTRNC(JNIEnv *jenv, jobject jobj)
{
    DCB     dcb;
    DWORD   lastRc;
    BOOL    success;
    HANDLE  osHandle = (HANDLE) w32getfd(jenv, jobj);
	if (osHandle == (HANDLE)-1) return -1;                    
	(void)memset(&dcb, 0, sizeof(DCB));
    success = GetCommState(osHandle,&dcb);
    if (!success) {
        lastRc = GetLastError();
        iveSerThrowWin(jenv,"GetCommState() failed",lastRc);
		printf("w32SerialPort_setDTRNC() failed to get the DCB: %d\n", success);
    }
	return (dcb.fDtrControl & DTR_CONTROL_ENABLE);
}  /* w32SerialPort_isDTRNC() */
/*
 * Function:   w32SerialPort_setRTSNC
 * Purpose:    setup the comm port RTS bit
 * Signature: (Ljava/lang/String;I)I
 * Return:     none	
 */
void w32SerialPort_setRTSNC(JNIEnv *jenv, jobject jobj, jboolean bool)
{
    DCB     dcb;
    DWORD   lastRc;
    BOOL    success;
    HANDLE  osHandle = (HANDLE) w32getfd(jenv, jobj);
	(void)memset(&dcb, 0, sizeof(DCB));
    success = GetCommState(osHandle,&dcb);
    if (!success) {
        lastRc = GetLastError();
        iveSerThrowWin(jenv,"GetCommState() failed",lastRc);
		printf("w32SerialPort_setDTRNC() failed to get the DCB: %d\n", success);
    }
	dcb.fRtsControl = RTS_CONTROL_ENABLE;
    success = SetCommState(osHandle,&dcb);
    if (!success) {
        lastRc = GetLastError();
        iveSerThrowWin(jenv,"SetCommState() failed",lastRc);
		printf("w32SerialPort_setDTRNC() failed to set the DCB: %d\n", success);
    }
}  /* w32SerialPort_setRTSNC() */
/*
 * Function:   w32SerialPort_isRTSNC
 * Purpose:    checking the current comm port RTS bit settings
 * Signature: (Ljava/lang/String;I)I
 * Return:     0 - Fail
 *			   none zero - succeeds	
 */
BOOL w32SerialPort_isRTSNC( JNIEnv *jenv, jobject jobj )
{
    DCB     dcb;
    DWORD   lastRc;
    BOOL    success;
    HANDLE  osHandle = (HANDLE) w32getfd(jenv, jobj);
	if (osHandle == (HANDLE)-1) return -1;                    
	(void)memset(&dcb, 0, sizeof(DCB));
    success = GetCommState(osHandle,&dcb);
    if (!success) {
        lastRc = GetLastError();
        iveSerThrowWin(jenv,"GetCommState() failed",lastRc);
		printf("w32SerialPort_isRTSNC() failed to get the DCB: %d\n", success);
    }
	return (dcb.fRtsControl&RTS_CONTROL_ENABLE);
}  /* w32SerialPort_isRTSNC() */
/*
 * Function:   w32SerialPort_isCTSNC
 * Purpose:    checking the current comm port CTS bit
 * Signature: (Ljava/lang/String;I)I
 * Return:     0 - Fail
 *			   none zero - succeeds	
 */
BOOL w32SerialPort_isCTSNC( JNIEnv *jenv, jobject jobj )
{
    DWORD	dwCommFlags = 0;
	HANDLE  osHandle = (HANDLE) w32getfd(jenv, jobj);
	if (osHandle == (HANDLE)-1) return -1;                    
	
	GetCommModemStatus( osHandle, &dwCommFlags );
	return  (dwCommFlags&MS_CTS_ON);
}  /* w32SerialPort_isCTSNC */
/*
 * Function:   w32SerialPort_isDSRNC
 * Purpose:    checking the current comm port DSR bit
 * Signature: (Ljava/lang/String;I)I
 * Return:     0 - Fail
 *			   none zero - succeeds	
 */
BOOL w32SerialPort_isDSRNC( JNIEnv *jenv, jobject jobj )
{
    DWORD	dwCommFlags = 0;
	HANDLE  osHandle = (HANDLE) w32getfd(jenv, jobj);
	if (osHandle == (HANDLE)-1) return -1;                    
	
	GetCommModemStatus( osHandle, &dwCommFlags );
	return  (dwCommFlags&MS_DSR_ON);
} /* w32SerialPort_isDSRNC() */
/*
 * Function:   w32SerialPort_isRINC
 * Purpose:    checking the current comm port RIN bit
 * Signature: (Ljava/lang/String;I)I
 * Return:     0 - Fail
 *			   none zero - succeeds	
 */
BOOL w32SerialPort_isRINC( JNIEnv *jenv, jobject jobj )
{
    DWORD	dwCommFlags = 0;
	HANDLE  osHandle = (HANDLE) w32getfd(jenv, jobj);
	if (osHandle == (HANDLE)-1) return -1;                    
	
	GetCommModemStatus( osHandle, &dwCommFlags );
	return  (dwCommFlags&MS_RING_ON);
}  /* w32SerialPort_isRINC() */
/*
 * Function:   w32SerialPort_isCDNC
 * Purpose:    checking the current comm port CD bit
 * Signature: (Ljava/lang/String;I)I
 * Return:     0 - Fail
 *			   none zero - succeeds	
 */
BOOL w32SerialPort_isCDNC(JNIEnv *jenv, jobject jobj)
{
    DWORD	dwCommFlags = 0;
	HANDLE  osHandle = (HANDLE) w32getfd(jenv, jobj);
	if (osHandle == (HANDLE)-1) return -1;                    
	
	GetCommModemStatus( osHandle, &dwCommFlags );
	return  (dwCommFlags&MS_RLSD_ON);
}  /* w32SerialPort_isCDNC() */
#if 0 //t.j
/*
 * Function:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    sendBreakNC
 * Signature: (II)I
 */
int w32SerialPort_sendBreakNC(JNIEnv *jenv, jobject jobj, jint jfd, jint jmillis) {
   (void)tcsendbreak(jfd, jmillis);
} /* w32SerialPort_sendBreakNC() */
#endif //t.j
/*
 * Function:   w32SerialPort_setSerialPortParamsNC
 * Purpose:    config the comm port
 * Signature: (Ljava/lang/String;I)I
 * Return:     0 - Fail
 *			   none zero - succeeds	
 */
int w32SerialPort_setSerialPortParamsNC
  (JNIEnv *jenv, jobject jobj, jint fd, jint jbaudrate, jint jdatabits, jint jstopbits, jint jparity ) {
    HANDLE       osHandle = (HANDLE) fd;
    DCB          dcb;
    COMMTIMEOUTS commTimeouts;
    BOOL         success;
    DWORD        lastRc;
    BYTE         parity;
    BYTE         stopBits;
//t.j    int          rts;
    switch (jparity) {
        case W32COMM_PARITY_EVEN:  parity = EVENPARITY;  break;
        case W32COMM_PARITY_ODD:   parity = ODDPARITY;   break;
        case W32COMM_PARITY_MARK:  parity = MARKPARITY;  break;
        case W32COMM_PARITY_SPACE: parity = SPACEPARITY; break;
        case W32COMM_PARITY_NONE:  
        default:                   parity = NOPARITY;    break;
    }
    switch (jstopbits) {
        case W32COMM_STOPBITS_1_5: stopBits = ONE5STOPBITS; break;
        case W32COMM_STOPBITS_2:   stopBits = TWOSTOPBITS;  break;
        case W32COMM_STOPBITS_1:   
        default:                   stopBits = ONESTOPBIT;   break;
    }
//t.j    if (config->hardwareFlowControl) {
//        rts = RTS_CONTROL_HANDSHAKE;
//    }
//    else {
//        rts = RTS_CONTROL_ENABLE;
//t.j    }
    memset(&dcb, 0, sizeof(DCB));
	success = GetCommState(osHandle,&dcb);
    if (!success) {
        lastRc = GetLastError();
        iveSerThrowWin(jenv,"GetCommState() failed",lastRc);
        return success;
    }
#ifdef DEBUG
//       LOG( ("pName: %s, %s, dName: %s, semID %d\n", pName,pp->portName, dName, semID) );
       printf( "before==> baudrate: %d, databits %d, stopbits: %d parity %d\n", dcb.BaudRate, dcb.ByteSize, dcb.StopBits, dcb.Parity );
       fflush( stdout );
#endif
	dcb.BaudRate          = jbaudrate;
    dcb.fBinary           = TRUE;
    dcb.fParity           = TRUE;
    dcb.fTXContinueOnXoff = FALSE;
    dcb.fErrorChar        = FALSE;
    dcb.fNull             = FALSE;
    dcb.fAbortOnError     = FALSE;
    dcb.ByteSize          = jdatabits;
    dcb.Parity            = parity;
    dcb.StopBits          = stopBits;
	dcb.fOutxCtsFlow      = FALSE; //(config->hardwareFlowControl != 0);
    dcb.fOutxDsrFlow      = FALSE; 
	dcb.fTXContinueOnXoff = FALSE;
    dcb.fOutX             = FALSE; //(config->softwareFlowControl != 0);
	dcb.fInX              = FALSE; //(config->softwareFlowControl != 0);
	dcb.XonChar           = 0;
	dcb.XoffChar		  = 0;
    success = SetCommState(osHandle,&dcb);
    if (!success) {
        lastRc = GetLastError();
        iveSerThrowWin(jenv,"SetCommState() failed",lastRc);
        return success;
    }
    memset(&commTimeouts,0,sizeof(commTimeouts));
    // this will wait for a char for 500ms and then return
    commTimeouts.ReadIntervalTimeout		 = MAXDWORD;
    commTimeouts.ReadTotalTimeoutConstant	 = 500;
    commTimeouts.ReadTotalTimeoutMultiplier	 = MAXDWORD;
    commTimeouts.WriteTotalTimeoutConstant	 = 0;
    commTimeouts.WriteTotalTimeoutMultiplier = 0;
    success = SetCommTimeouts(osHandle,&commTimeouts);
    if (!success) {
        lastRc = GetLastError();
        iveSerThrowWin(jenv,"SetCommTimeouts() failed",lastRc);
        return success;
    }
#ifdef DEBUG
//       LOG( ("pName: %s, %s, dName: %s, semID %d\n", pName,pp->portName, dName, semID) );
       printf( "after==> baudrate: %d, databits %d, stopbits: %d parity %d\n", dcb.BaudRate, dcb.ByteSize, dcb.StopBits, dcb.Parity );
       fflush( stdout );
#endif
    return success;
} /* w32SerialPort_setSerialPortParamsNC */
