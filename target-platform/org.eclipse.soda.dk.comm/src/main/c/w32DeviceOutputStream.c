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
#include "org_eclipse_soda_dk_comm_NSDeviceOutputStream.h"
#include "NSCommLOG.h"
#define assert(s) if (!s) {printf("\n\n%d asserted!\n\n", __LINE__); \
				     return(-1);}
/*
 * Class:     org_eclipse_soda_dk_comm_NSDeviceOutputStream
 * Method:    writeDeviceNC
 * Signature: ([BII)I
 */
int w32DeviceOutputStream_writeDeviceNC
  (JNIEnv *jenv, jobject jobj, jbyteArray jbuf, jint offset, jint length) {
    HANDLE      osHandle;
    BOOL        success;
#ifndef _WIN32_WCE
    OVERLAPPED  overlap; 
#endif
#if _WIN32_WCE>=400
	HANDLE		event;
#endif
    DWORD       lastRc;
    DWORD       actualBytes = 0;
    jclass	    jc;
    jfieldID	jf;
    jint 		fd = -1;
    jbyte		*cbuf;
    jboolean	isCopy;
    int		    writecnt = 0;
    jbyte		*cb;
#ifdef DEBUG
    printf("w32DeviceOutputStream_writeDeviceNC() entered\n");
#endif /* DEBUG */
    if (!length) return writecnt;
    /* Get the file descriptor. */
    jc = (*jenv)->GetObjectClass(jenv, jobj);
    assert(jc);
    jf = (*jenv)->GetFieldID(jenv, jc, "fd", "I");
    assert(jf);
    fd = (*jenv)->GetIntField(jenv, jobj, jf);
    if (fd == -1) return -1;                    
    osHandle = (HANDLE) fd;
    iveSerClearCommErrors(osHandle);
#if _WIN32_WCE>=400
    memset(&event,0,sizeof(event));
    event = CreateEvent(NULL,TRUE,FALSE,NULL);
    if (event == NULL) {
        lastRc = GetLastError();
        LOG(("iveSerWrite: error creating event semaphore: %d",lastRc));
        iveSerThrowWin(jenv,"Error creating event semaphore",lastRc);
        return 0;
    }
#else
    memset(&overlap,0,sizeof(overlap));
    overlap.hEvent = CreateEvent(NULL,TRUE,FALSE,NULL);
    if (NULL == overlap.hEvent) {
        lastRc = GetLastError();
        LOG(("iveSerWrite: error creating event semaphore: %d",lastRc));
        iveSerThrowWin(jenv,"Error creating event semaphore",lastRc);
        return 0;
    }
#endif //_WIN32_WCE>=400
    /* Convert the java byte array buffer into c byte buffer. */
    cbuf =  (*jenv)->GetByteArrayElements(jenv, jbuf, &isCopy);
    /* Write the data out to the device. */
    for ( cb = cbuf+offset; length; length -= actualBytes, writecnt += actualBytes, cb += actualBytes ) {
#if _WIN32_WCE>=400
        success = WriteFile(osHandle, cb, length, &actualBytes, 0);
#else
        success = WriteFile(osHandle, cb, length, &actualBytes, &overlap);
#endif //_WIN32_WCE
        if (success) {
#ifdef DEBUG
            printf("w32DeviceOutputStream_writeDeviceNC(): write completed successfully without blocking\n");
#endif /* DEBUG */
#if _WIN32_WCE>=400
            CloseHandle(event);
#else
            CloseHandle(overlap.hEvent);
#endif //_WIN32_WCE
            return actualBytes;
        }
        lastRc = GetLastError();
        if (ERROR_IO_PENDING != lastRc) {
#ifdef DEBUG
            printf("w32DeviceOutputStream_writeDeviceNC(): write failed: %d\n", lastRc);
#endif /* DEBUG */
#if _WIN32_WCE>=400
            CloseHandle(event);
#else
            CloseHandle(overlap.hEvent);
#endif //_WIN32_WCE
            iveSerThrowWin(jenv,"Error writing data",lastRc);
            return 0;
        }
        actualBytes = 0;
#ifndef _WIN32_WCE
        success = GetOverlappedResult(osHandle,&overlap,&actualBytes,TRUE);
#endif //_WIN32_WCE
        if (success) {
#ifdef DEBUG
            printf("w32DeviceOutputStream_writeDeviceNC(): getOverlappedResult completed successfully\n");
#endif /* DEBUG */
#if _WIN32_WCE>=400
            CloseHandle(event);
#else
            CloseHandle(overlap.hEvent);
#endif //_WIN32_WCE
            return actualBytes;
        }
        lastRc = GetLastError();
#ifdef DEBUG
        printf("w32DeviceOutputStream_writeDeviceNC(): getOverlappedResult failed: %d\n",lastRc);
#endif /* DEBUG */
#if _WIN32_WCE>=400
            CloseHandle(event);
#else
            CloseHandle(overlap.hEvent);
#endif //_WIN32_WCE
        iveSerThrowWin(jenv,"Error writing pending data",lastRc);
    }
    /* Should we throw some exception in the event of a write error ???? */
    /* Free the c byte buffer. */
    (*jenv)->ReleaseByteArrayElements(jenv, jbuf, cbuf, JNI_ABORT);
    return writecnt;
}	/* w32DeviceOutputStream_writeDeviceNC */
