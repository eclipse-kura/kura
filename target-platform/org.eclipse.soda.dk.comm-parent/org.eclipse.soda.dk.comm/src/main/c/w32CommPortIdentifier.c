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
#include "javax_comm_CommPortIdentifier.h"
#define assertexc(s)  if (!s) {fprintf(stderr, "\n\n%d asserted!\n\n", __LINE__); \
				 return(-1);}
#define NOOF_ELEMS(s)	((sizeof(s))/(sizeof(s[0])))
typedef struct port_s {
	char	*portName;
	int		semKey;
} port_t;
//#ifndef _POSIX_SEMAPHORES
//static struct sembuf	dev_wait[] = {
//		{ 0, 0, 0 }	/* wait until it is free */
//};
//#endif
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
#if 0 //t.j
static int GetSemID(const char *pnm) {
  int		semID = -1;
  port_t	port_tbl[] =  
	  {
			{ "LPT1", 0x11223344 },
			{ "COM1", 0x11223345 },
			{ "COM2", 0x11223347 },
			{ "COM3", 0x11223348 },
			{ "COM4", 0x11223349 },
			{ "COM5", 0x11223350 },
			{ "COM6", 0x11223351 },
			{ "COM7", 0x11223352 },
			{ "COM8", 0x11223353 },
			{ "COM9", 0x11223354 },
			{ "COM10", 0x11223355 },
			{ "COM11", 0x11223356 },
			{ "COM12", 0x11223357 },
			{ "COM13", 0x11223358 },
			{ "COM14", 0x11223359 },
			{ "COM15", 0x11223360 },
			{ "COM16", 0x11223361 },
			{ "COM17", 0x11223362 },
			{ "COM18", 0x11223363 },
			{ "COM19", 0x11223364 },
			{ "COM20", 0x11223365 },
		};
  port_t	*pp;
  int		keyFound = 0;
  /* Find the semaphore key for the corresponding port name. */
  for (pp = port_tbl;
       pp < port_tbl+NOOF_ELEMS(port_tbl);
       ++pp) {
      if (!strcmp(pp->portName, pnm)) {
	  keyFound++;
	  break;
      }
  }
  if (!keyFound)
      return semID;
#ifndef _POSIX_SEMAPHORES
  /* Get the semaphore ID for the key obtained. */
  semID = semget((key_t)pp->semKey, 1, 0);
#else
	/* don't worry about return value for right now */
  semID = sem_create(pp->semKey, 1);
#endif /* _POSIX_SEMAPHORES */
  return semID;
}	/* GetSemID() */
#endif //t.j
/*
 * Class:     javax_comm_CommPortIdentifier
 * Method:    monitorInterJVMDeviceAccessNC
 * Signature: (Ljava/lang/Thread;)I
 *
 * Currenty not Supported on Posix Devices
 */
int w32CommPortIdentifier_monitorInterJVMDeviceAccessNC
		(JNIEnv *jenv, jobject jobj, jobject jtho) {
	int		pollingTime;	/* seconds */
	int		oldVal, newVal;
	jclass		jc;
	jmethodID	jm;
	jfieldID	pnameID;
	jstring		pname;
	const char	*pnamec;
	jboolean	isInterruptedReturn;
	jclass		jcpoc;		/* CommPortOwnershipListener interf */
	jfieldID	cpoPOID;	/* PORT_OWNED ID */
	jfieldID	cpoPUID;	/* PORT_UNOWNED ID */
	jfieldID	cpoPRID;	/* PORT_OWNERSHIP_REQUESTED ID */
	jint		cpoPO;		/* PORT_OWNED value */
	jint		cpoPU;		/* PORT_UNOWNED value */
	jint		cpoPR;		/* PORT_OWNERSHIP_REQUESTED value */
	jmethodID	jintMethod;
	jclass		jthreadClass;
//t.j	int		semID;
//t.j	union semuni	scarg;
//	int		mypid = getpid();
//	int		scpid;
	pollingTime = getPollingTime(jenv);
	/* Get the class ID of the CommPortIdentifier object.*/
	jc = (*jenv)->GetObjectClass(jenv, jobj);
	assertexc(jc);
	/* Get the id of the method to report a change-ownership event. */
	jm = (*jenv)->GetMethodID(jenv, jc, "fireOwnershipEvent", "(I)V");
	assertexc(jm);
	/* Get the const values for the CommPortOwnershipListener events.*/
	jcpoc = (*jenv)->FindClass(jenv, "javax/comm/CommPortOwnershipListener");
	assertexc(jcpoc);
	cpoPOID = (*jenv)->GetStaticFieldID(jenv, jcpoc, "PORT_OWNED", "I");
	assertexc(cpoPOID);
	cpoPO = (*jenv)->GetStaticIntField(jenv, jcpoc, cpoPOID);
	cpoPUID = (*jenv)->GetStaticFieldID(jenv, jcpoc, "PORT_UNOWNED", "I");
	assertexc(cpoPUID);
	cpoPU = (*jenv)->GetStaticIntField(jenv, jcpoc, cpoPUID);
	cpoPRID = (*jenv)->GetStaticFieldID(jenv, jcpoc, "PORT_OWNERSHIP_REQUESTED", "I");
	assertexc(cpoPRID);
	cpoPR = (*jenv)->GetStaticIntField(jenv, jcpoc, cpoPRID);
	/* Get the port name. */
	pnameID = (*jenv)->GetFieldID(jenv, jc, "name", "Ljava/lang/String;");
	assertexc(pnameID);
	pname = (*jenv)->GetObjectField(jenv, jobj, pnameID);
	assertexc(pname);
  	pnamec = (*jenv)->GetStringUTFChars(jenv, pname, 0);
	/* Get the corresponding semaphore ID for the port name. */
//t.j	semID = GetSemID(pnamec);
//	if (semID == -1)
//t.j		return -1;
	(*jenv)->ReleaseStringUTFChars(jenv, pname, pnamec);
	/* Get access to the interrupted method. */
	jthreadClass = (*jenv)->FindClass(jenv, "java/lang/Thread");
	assertexc(jthreadClass);
	jintMethod = (*jenv)->GetMethodID(jenv, jthreadClass, "isInterrupted", "()Z");
	assertexc(jintMethod);
#if 0 //t.j
	(void)memset(&scarg, 0, sizeof(scarg));
/* what is this for? */
	/* Get the current value of the semaphore. */
#ifdef  _POSIX_SEMAPHORES
	if ((sem_getvalue(sem_lookup(semID), &oldVal)) < 0) {
#else
	if ((oldVal = semctl(semID, 0, GETVAL, scarg)) < 0) {
#endif	
		(void)fprintf(stderr, "Java_javax_comm_CommPortIdentifier_monitorInterJVMDeviceAccessNC: semctl error %d!\n", errno);
		return -1;
	}
#endif //t.j		
/* !!!!!!!!!!!!!! */
	while(1)
	{
		/* Check to see if this thread has been interrupted. */
		isInterruptedReturn = (*jenv)->CallBooleanMethod(jenv, jtho, jintMethod);
		if(isInterruptedReturn == JNI_TRUE)
			break;
		/* If the semaphore was locked the last time, wait until it
		   gets unlocked.  Else, catch some breath.
		 */
		Sleep(pollingTime);
		/* Get the new value of the semaphore. */
			/* Get the current value of the semaphore. */
#if 0 //t.j
#ifdef  _POSIX_SEMAPHORES
		if ((sem_getvalue(sem_lookup(semID), &oldVal)) < 0) {
#else
		if ((oldVal = semctl(semID, 0, GETVAL, scarg)) < 0) {
#endif		
			(void)fprintf(stderr, "Java_javax_comm_CommPortIdentifier_monitorInterJVMDeviceAccessNC: semctl error %d!\n", errno);
			return -1;
		}
		if (newVal == oldVal)
			continue;
		/* Get PID of the last process that changed the semaphore.
		   If it is the same JVM, ignore this change.
		 */
			/* Get the current value of the semaphore. */
#ifndef  _POSIX_SEMAPHORES
   /* DLS HACK needs to be changed */
		if ((scpid = semctl(semID, 0, GETPID, scarg)) < 0) {
			(void)fprintf(stderr, "Java_javax_comm_CommPortIdentifier_monitorInterJVMDeviceAccessNC: semctl error %d!\n", errno);
			return -1;
		}
		if (scpid != mypid) {
			/* If locked, send a PORT_OWNED event.
			   Else, send a PORT_UNOWNED event.
			 */
			(*jenv)->CallVoidMethod(jenv, jobj, jm,
						newVal ? cpoPO : cpoPU);
		}
#endif
#endif //t.j		
		oldVal = newVal;
	}	/* end of while() */
	return 0;
} /* w32CommPortIdentifier_monitorInterJVMDeviceAccessNC */
