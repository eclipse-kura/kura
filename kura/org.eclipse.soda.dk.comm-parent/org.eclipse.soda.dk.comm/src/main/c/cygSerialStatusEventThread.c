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
#include <errno.h>
#include <sys/ioctl.h>
#include <org_eclipse_soda_dk_comm_SerialStatusEventThread.h>
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
}	// getPollingTime()
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
/*
 * Class:     org_eclipse_soda_dk_comm_SerialStatusEventThread
 * Method:    monitorSerialStatusNC
 * Signature: (I)V
 */
void cygSerialStatusEventThread_monitorSerialStatusNC(JNIEnv *jenv, jobject jobj, jint jfd) {
	int		pollingTime;	/* seconds */
	int oldStatus, newStatus;
	jfieldID notifyOnCDID, notifyOnCTSID;
	jfieldID notifyOnDSRID, notifyOnRIID;
	jboolean notifyOnCDFlag = JNI_FALSE;
	jboolean notifyOnCTSFlag = JNI_FALSE;
	jboolean notifyOnDSRFlag = JNI_FALSE;
	jboolean notifyOnRIFlag = JNI_FALSE;
	jclass        jc;
	jmethodID     jm;
	jclass	 jspc;
	jfieldID 	 spID;
	jobject	 jsp;
	jboolean	 isInterruptedReturn;
	jclass	 jspec;	/* serial port event class */
	jfieldID	 speCDID,speCTSID, speDSRID, speRIID ;	/* field ID */
	jint		 speCD, speCTS, speDSR, speRI;	/* field value */
	jmethodID	 jintMethod;
	jclass	 jthreadClass;
	jint 		stopThreadFlag;
	
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
	if (ioctl(jfd, TIOCMGET, &oldStatus) < 0) {
		(void)fprintf(stderr, "Java_org_eclipse_soda_dk_comm_SerialStatusEventThread_monitorSerialStatusNC: ioctl error %d!\n", errno);
		return;
	}
	while(1)
	{
		sleep(pollingTime);
		stopThreadFlag = getStopThreadFlag(jenv, jobj);
		if (stopThreadFlag)
			break;
		/* check to see if this thread has been interrupted */
		isInterruptedReturn = (*jenv)->CallBooleanMethod(jenv,jobj,jintMethod);
		if(isInterruptedReturn == JNI_TRUE)
			break;
		notifyOnCDFlag = (*jenv)->GetBooleanField(jenv, jsp, notifyOnCDID);
		notifyOnCTSFlag = (*jenv)->GetBooleanField(jenv, jsp, notifyOnCTSID);
		notifyOnDSRFlag = (*jenv)->GetBooleanField(jenv, jsp, notifyOnDSRID);
		notifyOnRIFlag = (*jenv)->GetBooleanField(jenv, jsp, notifyOnRIID);
		if (ioctl(jfd, TIOCMGET, &newStatus) < 0) {
			(void)fprintf(stderr, "Java_org_eclipse_soda_dk_comm_SerialStatusEventThread_monitorSerialStatusNC: ioctl error %d!\n", errno);
			return;
		}
		if (newStatus == oldStatus)
			continue;
		if((newStatus & TIOCM_CD) != (oldStatus & TIOCM_CD))
		{
			if(notifyOnCDFlag)        /* need to use jsp to access this field */
				(*jenv)->CallVoidMethod(jenv, jsp, jm, speCD,
				    (oldStatus & TIOCM_CD)? JNI_TRUE:JNI_FALSE,(newStatus & TIOCM_CD)? JNI_TRUE:JNI_FALSE);
		}
		if((newStatus & TIOCM_CTS) != (oldStatus & TIOCM_CTS))
		{
			if(notifyOnCTSFlag)
				(*jenv)->CallVoidMethod(jenv, jsp, jm, speCTS,
				    (oldStatus & TIOCM_CTS)? JNI_TRUE:JNI_FALSE,(newStatus & TIOCM_CTS)? JNI_TRUE:JNI_FALSE);
		}
		if((newStatus & TIOCM_DSR) != (oldStatus & TIOCM_DSR))
		{
			if(notifyOnDSRFlag)
				(*jenv)->CallVoidMethod(jenv, jsp, jm, speDSR,
				    (oldStatus & TIOCM_DSR)? JNI_TRUE:JNI_FALSE,(newStatus & TIOCM_DSR)? JNI_TRUE:JNI_FALSE);
		}
		if((newStatus & TIOCM_RI) != (oldStatus & TIOCM_RI))
		{
			if(notifyOnRIFlag)
				(*jenv)->CallVoidMethod(jenv, jsp, jm, speRI,
				    (oldStatus & TIOCM_RI)? JNI_TRUE:JNI_FALSE,(newStatus & TIOCM_RI)? JNI_TRUE:JNI_FALSE);
		}
		oldStatus = newStatus;
	}	/* end of while() */
} /* cygSerialStatusEventThread_monitorSerialStatusNC */
