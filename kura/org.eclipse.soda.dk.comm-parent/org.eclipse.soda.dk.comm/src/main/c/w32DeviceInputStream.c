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
#ifndef _WIN32_WCE
#include <sys/types.h>
#endif
#include "org_eclipse_soda_dk_comm_NSDeviceInputStream.h"
#include "NSCommLOG.h"
#ifndef FALSE
#define FALSE 0
#endif
#ifndef TRUE
#define TRUE 1
#endif
#define READ_TIMEOUT 500		/* milliseconds */
#define assert(s)  if (!s) {printf("\n\n%d asserted!\n\n", __LINE__); \
				 return(-1);}
#define assertexc(s)    if (!s) {printf("\n\n%d asserted!\n\n", __LINE__); \
				 (*jenv)->ThrowNew(jenv, ec, "");}
/*
 * Function:   w32DeviceInputStream_readDeviceOneByteNC
 * Purpose:    Read one byte from serial port device 
 * Signature: ()I
 * Return:     0 - Fail
 *			   none zero - succeeds	
 */
int w32DeviceInputStream_readDeviceOneByteNC(JNIEnv *jenv, jobject jobj) {
    HANDLE      osHandle;
    BOOL        success;
    OVERLAPPED  overlap; 
    DWORD       lastRc;
    DWORD       actualBytes = 0;
	jclass		jc;
	jclass		ec;
	jfieldID	jf;
	jint 		fd = -1;
	char		buf[1];
	
#ifdef DEBUG
    printf("w32DeviceOutputStream_writeDeviceNC() entered\n");
#endif /* DEBUG */
	/* Get the exception class. */
	ec = (*jenv)->FindClass(jenv, "java/io/IOException");
	assert(ec);
	/* Get the file descriptor. */
	jc = (*jenv)->GetObjectClass(jenv, jobj);
	assertexc(jc);
	jf = (*jenv)->GetFieldID(jenv, jc, "fd", "I");
	assertexc(jf);
	fd = (*jenv)->GetIntField(jenv, jobj, jf);
	if (fd == -1) return -1;                    
    osHandle = (HANDLE) fd;
    iveSerClearCommErrors(osHandle);
    memset(&overlap,0,sizeof(overlap));
    overlap.hEvent = CreateEvent(NULL,TRUE,FALSE,NULL);
    if (NULL == overlap.hEvent) {
        lastRc = GetLastError();
        iveSerThrowWin(jenv,"Error creating event semaphore",lastRc);
        return -1;
    }
	success = ReadFile(osHandle, buf, 1, &actualBytes, &overlap);
	if(success) {
		CloseHandle(overlap.hEvent);
		return (int)(unsigned char)buf[0];
	} else {
		lastRc = GetLastError();
		if (ERROR_IO_PENDING != lastRc) {
			CloseHandle(overlap.hEvent);
			iveSerThrowWin(jenv,"Error reading data",lastRc);
			return -1;
		}
	}
	buf[0] = 0;
	actualBytes = 0;
#ifndef _WIN32_WCE
	if (WaitForSingleObject(overlap.hEvent,500) == WAIT_OBJECT_0) {
		success = GetOverlappedResult(osHandle,&overlap,&actualBytes,TRUE);
	}
	else {
		success = FALSE;
        }
#endif
	if ( success && actualBytes ) {
		lastRc = (int)(unsigned char)(buf[0]);
	} else {
		lastRc = -1;/*GetLastError();*/
	}
	CloseHandle(overlap.hEvent);
	return lastRc;
}	/* w32DeviceInputStream_readDeviceOneByteNC */
/*
 * Function:   w32DeviceInputStream_readDeviceNC
 * Purpose:    Read Serial Port Device
 * Signature: ([BII)I
 * Return:     0 - Fail
 *			   none zero - succeeds	
 */
int w32DeviceInputStream_readDeviceNC
  (JNIEnv *jenv, jobject jobj, jbyteArray jba, jint off, jint len) {
    HANDLE      osHandle;
    BOOL        success;
    OVERLAPPED  overlap; 
    DWORD       lastRc;
    DWORD       actualBytes = 0;
	jclass		jc;
	jfieldID	jf;
	jint 		fd = -1;
	jbyte		*cbuf;
    jboolean	isCopy;
    jfieldID	tmof;
    int		    tmo;
    jfieldID	tmoDonef;
	DWORD eventMask;
    HGLOBAL     hglob;
    hglob = GlobalAlloc(GMEM_MOVEABLE, len);
    if (!hglob)
    {
        return (void *) 0;
    }
    cbuf = GlobalLock(hglob);
    if (!cbuf) {
        GlobalFree(hglob);
    }
#ifdef DEBUG
    printf("w32DeviceInputStream_readDeviceNC() entered\n");
#endif /* DEBUG */
	/* Get the file descriptor. */
	jc = (*jenv)->GetObjectClass(jenv, jobj);
	assert(jc);
	jf = (*jenv)->GetFieldID(jenv, jc, "fd", "I");
	assert(jf);
	fd = (*jenv)->GetIntField(jenv, jobj, jf);
	if (fd == -1) return -1;
	osHandle = (HANDLE) fd;
    tmof = (*jenv)->GetFieldID(jenv, jc, "tmo", "I");
    assert(tmof);
    tmoDonef = (*jenv)->GetFieldID(jenv, jc, "tmoDone", "Z");
    assert(tmoDonef);
    tmo = (*jenv)->GetIntField(jenv, jobj, tmof);
    iveSerClearCommErrors(osHandle);
    memset(&overlap,0,sizeof(overlap));
    overlap.hEvent = CreateEvent(NULL,TRUE,FALSE,NULL);
    if (NULL == overlap.hEvent) {
        lastRc = GetLastError();
        iveSerThrowWin(jenv,"Error creating event semaphore",lastRc);
        
        // Free the bufffer memory
        hglob = GlobalHandle( cbuf );
        if (hglob) {
            GlobalUnlock( hglob );
            GlobalFree( hglob );
        }   	
        return lastRc;
    }
    success = ReadFile(osHandle, cbuf, len, &actualBytes, &overlap);
	if(success) {
	  // Copy back the data into the java buffer.
	  if (actualBytes > 0)
		(*jenv)->SetByteArrayRegion(jenv, jba, off, actualBytes, (jbyte*)cbuf);
        // Free the bufffer memory
        hglob = GlobalHandle( cbuf );
        if (hglob) {
            GlobalUnlock( hglob );
            GlobalFree( hglob );
        }   	
		CloseHandle(overlap.hEvent);
		return actualBytes;
	} else {
		lastRc = GetLastError();
		if (ERROR_IO_PENDING != lastRc) {
			CloseHandle(overlap.hEvent);
			iveSerThrowWin(jenv,"Error reading data",lastRc);
			(*jenv)->SetBooleanField(jenv, jobj, tmoDonef, (jboolean)JNI_TRUE);
	
        // Free the bufffer memory
	        hglob = GlobalHandle( cbuf );
	        if (hglob) {
	            GlobalUnlock( hglob );
	            GlobalFree( hglob );
	        } 
				
			return lastRc;
		}
	}
	
	actualBytes = 0;
#ifndef _WIN32_WCE
	success = GetOverlappedResult(osHandle,&overlap,&actualBytes,TRUE);
#endif
	if (success) {
	  // Copy back the data into the java buffer.
	  if (actualBytes > 0)
		(*jenv)->SetByteArrayRegion(jenv, jba, off, actualBytes, (jbyte*)cbuf);
		
		lastRc = actualBytes;
	} else {
		lastRc = GetLastError();
	}
	//Free the bufffer memory
    hglob = GlobalHandle( cbuf );
    if (hglob) {
        GlobalUnlock( hglob );
        GlobalFree( hglob );
    }   
    CloseHandle(overlap.hEvent);
	return lastRc;
}  /* w32DeviceInputStream_readDeviceNC() */
/*
 * Function:   w32DeviceInputStream_getReadCountNC
 * Purpose:    Check serial port device event 
 * Signature: ()I
 * Return:     0 - Fail
 *			   none zero - succeeds	
 */
int w32DeviceInputStream_getReadCountNC(JNIEnv *jenv, jobject jobj)
{
    DWORD	dwCommEvent = 0;
	HANDLE  osHandle;
	
	jclass		jc;
	jclass		ec;
	jfieldID	jf;
	jint 		fd = -1;
	int			dc = 0;
	// Get the exception class.
	ec = (*jenv)->FindClass(jenv, "java/io/IOException");
	assert(ec);
	// Get the file descriptor.
	jc = (*jenv)->GetObjectClass(jenv, jobj);
	assertexc(jc);
	jf = (*jenv)->GetFieldID(jenv, jc, "fd", "I");
	assertexc(jf);
	fd = (*jenv)->GetIntField(jenv, jobj, jf);
	if (fd == -1) {
		(*jenv)->ThrowNew(jenv, ec, "");
	}
	osHandle = (HANDLE)fd;
	if(!GetCommMask( osHandle, &dwCommEvent ))
		return GetLastError();
	
	dwCommEvent |= EV_RXCHAR;
	if( !SetCommMask( osHandle, dwCommEvent ))
		return GetLastError();
	if( !WaitCommEvent( osHandle, &dwCommEvent, NULL ))
		return GetLastError();
	return (dwCommEvent&EV_RXCHAR);
}  /* w32DeviceInputStream_getReadCountNC() */
