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
#endif
#include "org_eclipse_soda_dk_comm_SerialStatusEventThread.h"
#define assertexc(s) if (!s) {printf("\n\n%d asserted!\n\n", __LINE__); return;}
static int	getPollingTime(JNIEnv *jenv) {
  int		ptime = 5;
  jclass	cic;
  jfieldID	ptID;
  jint		pt;
  do {
	cic = (*jenv)->FindClass(jenv, "javax/comm/CommPortIdentifier");
	if (!cic) break;
	ptID = (*jenv)->GetStaticFieldID(jenv, cic, "pollingTime", "I");
	if (!ptID) break;
	pt = (*jenv)->GetStaticIntField(jenv, cic, ptID);
	if (pt > 0)
		ptime = pt;
  } while (0);
  return ptime;
}	/* getPollingTime() */
/*
 * Class:     org_eclipse_soda_dk_comm_SerialStatusEventThread
 * Method:    monitorSerialStatusNC
 * Signature: (I)V
 */
void w32SerialStatusEventThread_monitorSerialStatusNC
		(JNIEnv *jenv, jobject jobj, jint jfd) {
	int		 pollingTime;								/* seconds */
	HANDLE   osHandle = (HANDLE)jfd;
	DWORD	 oldStatus, newStatus;
	jfieldID notifyOnCDID, notifyOnCTSID;
	jfieldID notifyOnDSRID, notifyOnRIID;
	jboolean notifyOnCDFlag = JNI_FALSE;
	jboolean notifyOnCTSFlag = JNI_FALSE;
	jboolean notifyOnDSRFlag = JNI_FALSE;
	jboolean notifyOnRIFlag = JNI_FALSE;
	jclass      jc;
	jmethodID   jm;
	jclass		jspc;
	jfieldID 	spID;
	jobject		jsp;
	jboolean	isInterruptedReturn;
	jclass		jspec;									/* serial port event class */
	jfieldID	speCDID,speCTSID, speDSRID, speRIID ;	/* field ID		*/
	jint		speCD, speCTS, speDSR, speRI;			/* field value	*/
	jmethodID	jintMethod;
	jclass		jthreadClass;
	pollingTime = getPollingTime(jenv);
	/* Get the const values for all the serial port event types.*/
	jspec = (*jenv)->FindClass(jenv, "javax/comm/SerialPortEvent");
	assertexc(jspec);
	speCDID = (*jenv)->GetStaticFieldID(jenv, jspec, "CD", "I");
	assertexc(speCDID);
	speCD = (*jenv)->GetStaticIntField(jenv, jspec, speCDID);
	speCTSID = (*jenv)->GetStaticFieldID(jenv, jspec, "CTS", "I");
	assertexc(speCTSID);
	speCTS = (*jenv)->GetStaticIntField(jenv, jspec, speCTSID);
	speDSRID = (*jenv)->GetStaticFieldID(jenv, jspec, "DSR", "I");
	assertexc(speDSRID);
	speDSR = (*jenv)->GetStaticIntField(jenv, jspec, speDSRID);
	speRIID = (*jenv)->GetStaticFieldID(jenv, jspec, "RI", "I");
	assertexc(speRIID);
	speRI = (*jenv)->GetStaticIntField(jenv, jspec, speRIID);
	/* Get the serial port object.*/
	jc = (*jenv)->GetObjectClass(jenv, jobj);
	assertexc(jc);
	spID = (*jenv)->GetFieldID(jenv, jc, "serialPort", "Lorg/eclipse/soda/dk/comm/NSSerialPort;");
	assertexc(spID);
	jsp = (*jenv)->GetObjectField(jenv, jobj, spID);
	assertexc(jsp);
	/* Get the class ID of the serial port object.*/
	jspc = (*jenv)->GetObjectClass(jenv, jsp);
	assertexc(jspc);
	notifyOnCDID = (*jenv)->GetFieldID(jenv, jspc, "notifyOnCDFlag", "Z");
	assertexc(notifyOnCDID);
	notifyOnCTSID = (*jenv)->GetFieldID(jenv, jspc, "notifyOnCTSFlag", "Z");
	assertexc(notifyOnCTSID);
	notifyOnDSRID = (*jenv)->GetFieldID(jenv, jspc, "notifyOnDSRFlag", "Z");
	assertexc(notifyOnDSRID);
	notifyOnRIID = (*jenv)->GetFieldID(jenv, jspc, "notifyOnRIFlag", "Z");
	assertexc(notifyOnRIID);
	/* Get access to the method to add a port.*/
	jm = (*jenv)->GetMethodID(jenv, jspc, "reportSerialEvent", "(IZZ)V");
	assertexc(jm);
	/* Get access to the interrupted method.*/
	jthreadClass = (*jenv)->FindClass(jenv, "java/lang/Thread");
	assertexc(jthreadClass);
	jintMethod = (*jenv)->GetMethodID(jenv, jthreadClass, "isInterrupted", "()Z");
	assertexc(jintMethod);
//	if (ioctl(jfd, TIOCMGET, &oldStatus) < 0) {
//		(void)fprintf(stderr, "Java_org_eclipse_soda_dk_comm_SerialStatusEventThread_monitorSerialStatusNC: ioctl error %d!\n", errno);
//		return;
//t.j	}
#ifndef _WIN32_WCE
	if (GetCommModemStatus(osHandle, &oldStatus) < 0) {
		errno = GetLastError();
		(void)fprintf(stderr, "w32SerialStatusEventThread_monitorSerialStatusNC: GetCommModemStatus() error %d!\n", errno);
		return;
	}
#endif //_WIN32_WCE
	while(1)
	{
		Sleep(pollingTime);
		/* check to see if this thread has been interrupted */
		isInterruptedReturn = (*jenv)->CallBooleanMethod(jenv,jobj,jintMethod);
		if(isInterruptedReturn == JNI_TRUE)
			break;
		notifyOnCDFlag  = (*jenv)->GetBooleanField(jenv, jsp, notifyOnCDID);
		notifyOnCTSFlag = (*jenv)->GetBooleanField(jenv, jsp, notifyOnCTSID);
		notifyOnDSRFlag = (*jenv)->GetBooleanField(jenv, jsp, notifyOnDSRID);
		notifyOnRIFlag  = (*jenv)->GetBooleanField(jenv, jsp, notifyOnRIID);
#ifndef _WIN32_WCE
		if (GetCommModemStatus(osHandle, &newStatus) < 0) {
			errno = GetLastError();
			(void)fprintf(stderr, "w32SerialStatusEventThread_monitorSerialStatusNC: ioctl error %d!\n", errno);
			return;
		}
#endif //_WIN32_WCE
		if (newStatus == oldStatus)
			continue;
		if((newStatus & MS_RLSD_ON) != (oldStatus & MS_RLSD_ON))
		{
			if(notifyOnCDFlag)        /* need to use jsp to access this field */
				(*jenv)->CallVoidMethod(jenv, jsp, jm, speCD,
				    (oldStatus & MS_RLSD_ON)? JNI_TRUE:JNI_FALSE,(newStatus & MS_RLSD_ON)? JNI_TRUE:JNI_FALSE);
		}
		if((newStatus & MS_CTS_ON) != (oldStatus & MS_CTS_ON))
		{
			if(notifyOnCTSFlag)
				(*jenv)->CallVoidMethod(jenv, jsp, jm, speCTS,
				    (oldStatus & MS_CTS_ON)? JNI_TRUE:JNI_FALSE,(newStatus & MS_CTS_ON)? JNI_TRUE:JNI_FALSE);
		}
		if((newStatus & MS_DSR_ON) != (oldStatus & MS_DSR_ON))
		{
			if(notifyOnDSRFlag)
				(*jenv)->CallVoidMethod(jenv, jsp, jm, speDSR,
				    (oldStatus & MS_DSR_ON)? JNI_TRUE:JNI_FALSE,(newStatus & MS_DSR_ON)? JNI_TRUE:JNI_FALSE);
		}
		if((newStatus & MS_RING_ON) != (oldStatus & MS_RING_ON))
		{
			if(notifyOnRIFlag)
				(*jenv)->CallVoidMethod(jenv, jsp, jm, speRI,
				    (oldStatus & MS_RING_ON)? JNI_TRUE:JNI_FALSE,(newStatus & MS_RING_ON)? JNI_TRUE:JNI_FALSE);
		}
		oldStatus = newStatus;
	}	/* end of while() */
} /* w32SerialStatusEventThread_monitorSerialStatusNC */
