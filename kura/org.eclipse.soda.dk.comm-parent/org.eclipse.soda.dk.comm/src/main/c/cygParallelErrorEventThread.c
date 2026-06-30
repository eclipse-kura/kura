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
#ifdef NCI
#include <dev/ic/lptioctl.h>
#include <dev/ic/lptreg.h>
#endif	/* NCI */
#ifdef __linux__
//#define __KERNEL__  /* For printer error definitions */
#include <linux/lp.h>
#endif	/* __linux__ */
#include <org_eclipse_soda_dk_comm_ParallelErrorEventThread.h>
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
 * Class:     org_eclipse_soda_dk_comm_ParallelErrorEventThread
 * Method:    monitorParallelErrorNC
 * Signature: (I)V
 */
void cygParallelErrorEventThread_monitorParallelErrorNC
    (JNIEnv *jenv, jobject jobj, jint jfd) {
	int		pollingTime;	/* seconds */
	int		oldStatus, newStatus;
	jfieldID	notifyOnErrorID;
	jboolean	notifyOnErrorFlag = JNI_TRUE;
	jclass		jc;
	jmethodID	jm;
	jclass		jppc;
	jfieldID	ppID;
	jobject		jpp;
	jboolean	isInterruptedReturn;
	jclass		jppec;		/* parallel port event class */
	jfieldID	ppeErrorID;	/* field ID */
	jint		ppeError;	/* field value */
	jmethodID	jintMethod;
	jclass		jthreadClass;
	
	jint 		stopThreadFlag;
	pollingTime = getPollingTime(jenv);
	/* Get the const value for the parallel port error event type.*/
	jppec = (*jenv)->FindClass(jenv, "javax/comm/ParallelPortEvent");
	assertexc(jppec);
	ppeErrorID = (*jenv)->GetStaticFieldID(jenv, jppec, "PAR_EV_ERROR", "I");
	assertexc(ppeErrorID);
	ppeError = (*jenv)->GetStaticIntField(jenv, jppec, ppeErrorID);
	/* Get the parallel port object.*/
	jc = (*jenv)->GetObjectClass(jenv, jobj);
	assertexc(jc);
	ppID = (*jenv)->GetFieldID(jenv, jc, "pp", "Lorg/eclipse/soda/dk/comm/NSParallelPort;");
	assertexc(ppID);
	jpp = (*jenv)->GetObjectField(jenv, jobj, ppID);
	assertexc(jpp);
	/* Get the class ID of the parallel port object.*/
	jppc = (*jenv)->GetObjectClass(jenv, jpp);
	assertexc(jppc);
	/* Get the notify flag field ID of the parallel port object. */
	notifyOnErrorID = (*jenv)->GetFieldID(jenv, jppc, "notifyOnErrorFlag", "Z");
	assertexc(notifyOnErrorID);
	/* Get access to the method to report a parallel event.*/
	jm = (*jenv)->GetMethodID(jenv, jppc, "reportParallelEvent", "(IZZ)V");
	assertexc(jm);
	/* Get access to the interrupted method.*/
	jthreadClass = (*jenv)->FindClass(jenv, "java/lang/Thread");
	assertexc(jthreadClass);
	jintMethod = (*jenv)->GetMethodID(jenv, jthreadClass, "isInterrupted", "()Z");
	assertexc(jintMethod);
#ifdef NCI
	if (ioctl(jfd, PIOCSTATUS, &oldStatus) < 0) {
#endif	/* NCI */
#ifdef __osx__      /* ToDo: implement */
        oldStatus = 0;
	if (0) {
#endif	/* __osx__ */
#ifdef __linux__
	if (ioctl(jfd, LPGETSTATUS, &oldStatus) < 0) {
#endif	/* __linux__ */
#ifdef QNX      /* ToDo: implement */
        oldStatus = 0;
	if (0) {
#endif	/* QNX */
		(void)fprintf(stderr, "Java_org_eclipse_soda_dk_comm_ParallelErrorEventThread_monitorParallelErrorNC: ioctl error %d!\n", errno);
		return;
	}
	while(1)
	{
		sleep(pollingTime);
		stopThreadFlag = getStopThreadFlag(jenv, jobj);
		if (stopThreadFlag)
			break;
			
		/* check to see if this thread has been interrupted */
		isInterruptedReturn = (*jenv)->CallBooleanMethod(jenv, jobj, jintMethod);
		if(isInterruptedReturn == JNI_TRUE)
			break;
		/* Get the notify error flag. If not set, skip error checks. */
		notifyOnErrorFlag = (*jenv)->GetBooleanField(jenv, jpp, notifyOnErrorID);
		if (notifyOnErrorFlag != JNI_TRUE)
			continue;
#ifdef NCI
		if (ioctl(jfd, PIOCSTATUS, &newStatus) < 0) {
#endif	/* NCI */
#ifdef __osx__      /* ToDo: implement */
                newStatus = 0;
	        if (0) {
#endif	/* __osx__ */
#ifdef __linux__
		if (ioctl(jfd, LPGETSTATUS, &newStatus) < 0) {
#endif	/* __linux__ */
#ifdef QNX      /* ToDo: implement */
                newStatus = 0;
	        if (0) {
#endif	/* QNX */
			(void)fprintf(stderr, "Java_org_eclipse_soda_dk_comm_ParallelErrorEventThread_monitorParallelErrorNC: ioctl error %d!\n", errno);
			return;
		}
		if (newStatus == oldStatus)
			continue;
#ifdef NCI
		if((newStatus & LPS_NERR) != (oldStatus & LPS_NERR))
#endif	/* NCI */
#ifdef __osx__      /* ToDo: implement */
	        if (0)
#endif	/* __osx__ */
#ifdef __linux__
		if((newStatus & LP_PERRORP) != (oldStatus & LP_PERRORP))
#endif	/* __linux__ */
#ifdef QNX      /* ToDo: implement */
	        if (0)
#endif	/* QNX */
		{
			(*jenv)->CallVoidMethod(jenv, jpp, jm, ppeError,
#ifdef NCI
				    (oldStatus & LPS_NERR)? JNI_TRUE:JNI_FALSE,
				    (newStatus & LPS_NERR)? JNI_TRUE:JNI_FALSE);
#endif	/* NCI */
#ifdef __osx__      /* ToDo: implement */
	                            JNI_FALSE, JNI_FALSE);
#endif	/* __osx__ */
#ifdef __linux__
				    (oldStatus & LP_PERRORP)? JNI_TRUE:JNI_FALSE,
				    (newStatus & LP_PERRORP)? JNI_TRUE:JNI_FALSE);
#endif	/* __linux__ */
#ifdef QNX      /* ToDo: implement */
	                            JNI_FALSE, JNI_FALSE);
#endif	/* QNX */
			oldStatus = newStatus;
			continue;
		}
#ifdef NCI
		if((newStatus & LPS_SELECT) != (oldStatus & LPS_SELECT))
#endif	/* NCI */
#ifdef __osx__      /* ToDo: implement */
	        if (0)
#endif	/* __osx__ */
#ifdef __linux__
		if((newStatus & LP_PSELECD) != (oldStatus & LP_PSELECD))
#endif	/* __linux__ */
#ifdef QNX      /* ToDo: implement */
	        if (0)
#endif	/* QNX */
		{
			(*jenv)->CallVoidMethod(jenv, jpp, jm, ppeError,
#ifdef NCI
				    (oldStatus & LPS_SELECT)? JNI_TRUE:JNI_FALSE,
				    (newStatus & LPS_SELECT)? JNI_TRUE:JNI_FALSE);
#endif	/* NCI */
#ifdef __osx__      /* ToDo: implement */
	                            JNI_FALSE, JNI_FALSE);
#endif	/* __osx__ */
#ifdef __linux__
				    (oldStatus & LP_PSELECD)? JNI_TRUE:JNI_FALSE,
				    (newStatus & LP_PSELECD)? JNI_TRUE:JNI_FALSE);
#endif	/* __linux__ */
#ifdef QNX      /* ToDo: implement */
	                            JNI_FALSE, JNI_FALSE);
#endif	/* QNX */
			oldStatus = newStatus;
			continue;
		}
#ifdef NCI
		if((newStatus & LPS_NOPAPER) != (oldStatus & LPS_NOPAPER))
#endif	/* NCI */
#ifdef __osx__      /* ToDo: implement */
	        if (0)
#endif	/* __osx__ */
#ifdef __linux__
		if((newStatus & LP_POUTPA) != (oldStatus & LP_POUTPA))
#endif	/* __linux__ */
#ifdef QNX      /* ToDo: implement */
	        if (0)
#endif	/* QNX */
		{
			(*jenv)->CallVoidMethod(jenv, jpp, jm, ppeError,
#ifdef NCI
				    (oldStatus & LPS_NOPAPER)? JNI_TRUE:JNI_FALSE,
				    (newStatus & LPS_NOPAPER)? JNI_TRUE:JNI_FALSE);
#endif	/* NCI */
#ifdef __osx__      /* ToDo: implement */
	                            JNI_FALSE, JNI_FALSE);
#endif	/* __osx__ */
#ifdef __linux__
				    (oldStatus & LP_POUTPA)? JNI_TRUE:JNI_FALSE,
				    (newStatus & LP_POUTPA)? JNI_TRUE:JNI_FALSE);
#endif	/* __linux__ */
#ifdef QNX      /* ToDo: implement */
	                            JNI_FALSE, JNI_FALSE);
#endif	/* QNX */
			oldStatus = newStatus;
			continue;
		}
#ifdef NCI
		if((newStatus & LPS_NACK) != (oldStatus & LPS_NACK))
#endif	/* NCI */
#ifdef __osx__      /* ToDo: implement */
	        if (0)
#endif	/* __osx__ */
#ifdef __linux__
		if((newStatus & LP_PACK) != (oldStatus & LP_PACK))
#endif	/* __linux__ */
#ifdef QNX      /* ToDo: implement */
	        if (0)
#endif	/* QNX */
		{
			(*jenv)->CallVoidMethod(jenv, jpp, jm, ppeError,
#ifdef NCI
				    (oldStatus & LPS_NACK)? JNI_TRUE:JNI_FALSE,
				    (newStatus & LPS_NACK)? JNI_TRUE:JNI_FALSE);
#endif	/* NCI */
#ifdef __osx__      /* ToDo: implement */
	                            JNI_FALSE, JNI_FALSE);
#endif	/* __osx__ */
#ifdef __linux__
				    (oldStatus & LP_PACK)? JNI_TRUE:JNI_FALSE,
				    (newStatus & LP_PACK)? JNI_TRUE:JNI_FALSE);
#endif	/* __linux__ */
#ifdef QNX      /* ToDo: implement */
	                            JNI_FALSE, JNI_FALSE);
#endif	/* QNX */
			oldStatus = newStatus;
			continue;
		}
#ifdef NCI
		if((newStatus & LPS_NBSY) != (oldStatus & LPS_NBSY))
#endif	/* NCI */
#ifdef __osx__      /* ToDo: implement */
	        if (0)
#endif	/* __osx__ */
#ifdef __linux__
		if((newStatus & LP_PBUSY) != (oldStatus & LP_PBUSY))
#endif	/* __linux__ */
#ifdef QNX      /* ToDo: implement */
	        if (0)
#endif	/* QNX */
		{
			(*jenv)->CallVoidMethod(jenv, jpp, jm, ppeError,
#ifdef NCI
				    (oldStatus & LPS_NBSY)? JNI_TRUE:JNI_FALSE,
				    (newStatus & LPS_NBSY)? JNI_TRUE:JNI_FALSE);
#endif	/* NCI */
#ifdef __osx__      /* ToDo: implement */
	                            JNI_FALSE, JNI_FALSE);
#endif	/* __osx__ */
#ifdef __linux__
				    (oldStatus & LP_PBUSY)? JNI_TRUE:JNI_FALSE,
				    (newStatus & LP_PBUSY)? JNI_TRUE:JNI_FALSE);
#endif	/* __linux__ */
#ifdef QNX      /* ToDo: implement */
	                            JNI_FALSE, JNI_FALSE);
#endif	/* QNX */
			oldStatus = newStatus;
			continue;
		}
		oldStatus = newStatus;
	}	/* end of while() */
} /* cygParallelErrorEventThread_monitorParallelErrorNC */
