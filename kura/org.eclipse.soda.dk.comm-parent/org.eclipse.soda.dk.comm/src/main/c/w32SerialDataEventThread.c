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
#include <windows.h>
#include <winsock2.h>
#ifndef _WIN32_WCE
#include <errno.h>
#else
#include <winsock2.h>
#endif
#include "org_eclipse_soda_dk_comm_SerialDataEventThread.h"
#define assertexc(s)       if (!s) {printf("\n\n%d asserted!\n\n", __LINE__); \
				 return;}
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
#if _WIN32_WCE>=400
static int getStopThreadFlag(JNIEnv *jenv, jobject jobj)
{
	jclass cls;
	jfieldID fid;
	jint stopThreadFlag;
	
	cls = (*jenv)->GetObjectClass(jenv, jobj);
	if (!cls) (*jenv)->FatalError(jenv, "Missing class");
	
	fid = (*jenv)->GetFieldID(jenv, cls, "stopThreadFlag", "I");
	if (fid == NULL) (*jenv)->FatalError(jenv, "Missing field");
	
	stopThreadFlag = (*jenv)->GetIntField(jenv, jobj, fid);
	
	return stopThreadFlag;	
}
#endif //_WIN32_WCE>=400
/*
 * Class:     org_eclipse_soda_dk_comm_SerialDataEventThread
 * Method:    monitorSerialDataNC
 * Signature: (I)V
 */
void w32SerialDataEventThread_monitorSerialDataNC
  (JNIEnv *jenv, jobject jobj, jint jfd) {
    jclass		jspec;					/* serial port event class */
    fd_set		r_mask;
    jfieldID	data_available_id;      /* field ID */
    jint		data_available_event;	/* field value */
    jclass      jc;
    jmethodID   jm;
    jfieldID    spID;
    jobject		jsp;
    jclass		jspc;
    jfieldID	notifyOnDataAvailableID;
    jboolean	notifyOnDataAvailableFlag = JNI_FALSE;
    int			result;
	int			pollingTime;			/* seconds */
	struct		timeval	tv;
	jboolean	isInterruptedReturn;
	jclass		jthreadClass;
	jmethodID	jintMethod;
#if _WIN32_WCE>=400
	jint 		stopThreadFlag;
#endif
	pollingTime = getPollingTime(jenv);
    jspec = (*jenv)->FindClass(jenv, "javax/comm/SerialPortEvent");
    assertexc(jspec);
    data_available_id = (*jenv)->GetStaticFieldID(jenv, jspec, "DATA_AVAILABLE", "I");
    assertexc(data_available_id);
    data_available_event = (*jenv)->GetStaticIntField(jenv, jspec, data_available_id);
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
    /* Get access to the method to add a port.*/
    jm = (*jenv)->GetMethodID(jenv, jspc, "reportSerialEvent", "(IZZ)V");
    assertexc(jm);
    notifyOnDataAvailableID = (*jenv)->GetFieldID(jenv, jspc, "notifyOnDataFlag", "Z");
    assertexc(notifyOnDataAvailableID);
	/* Get access to the interrupted method.*/
	jthreadClass = (*jenv)->FindClass(jenv, "java/lang/Thread");
	assertexc(jthreadClass);
	jintMethod = (*jenv)->GetMethodID(jenv, jthreadClass, "isInterrupted", "()Z");
	assertexc(jintMethod);
	while(1)
	{
#if _WIN32_WCE>=400
		stopThreadFlag = getStopThreadFlag(jenv, jobj);
		if (stopThreadFlag)
			break;	 	
#endif
		/* check to see if this thread has been interrupted */
		isInterruptedReturn = (*jenv)->CallBooleanMethod(jenv,jobj,jintMethod);
		if(isInterruptedReturn == JNI_TRUE)
			break;
		memset(&tv, 0, sizeof(tv));
		tv.tv_sec = pollingTime;
		FD_ZERO(&r_mask);
		FD_SET(jfd, &r_mask);
		result = select(jfd + 1, &r_mask, NULL, NULL, &tv);
#ifndef _WIN32_WCE
		if (result == -1 && errno != EINTR) break;
#endif
		if (!result)  continue;
		if (FD_ISSET(jfd, &r_mask))
		{
			notifyOnDataAvailableFlag = (*jenv)->GetBooleanField(jenv, jsp, notifyOnDataAvailableID);
			if(notifyOnDataAvailableFlag)
				(*jenv)->CallVoidMethod(jenv, jsp, jm, data_available_event, JNI_TRUE, JNI_TRUE);
        }
    }
} /* w32SerialDataEventThread_monitorSerialDataNC */
