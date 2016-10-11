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
#include "dkcomm.h"
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <sys/types.h>
#include <unistd.h>
#ifdef _POSIX_SEMAPHORES
#include <semaphore.h>
#include "SysVStyleSemaphore.h"
#else 
#include <sys/ipc.h> 
#include <sys/sem.h> 
#endif 
  
#include <javax_comm_CommPortIdentifier.h>
#define assertexc(s)       if (!s) {fprintf(stderr, "\n\n%d asserted!\n\n", __LINE__); \
				 return(-1);}
#define NOOF_ELEMS(s)	((sizeof(s))/(sizeof(s[0])))
typedef struct port_s {
	char		*portName;
	int		semKey;
} port_t;
#ifndef _POSIX_SEMAPHORES
static struct sembuf	dev_wait[] = {
		{ 0, 0, 0 }	/* wait until it is free */
};
#endif
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
static int GetSemID(const char *pnm) {
  int		semID = -1;
  port_t	port_tbl[] =  
	  {
			{ "LPT1", 0x11223344 },
			{ "COM1", 0x11223345 },
#ifdef NCI
			{ "SC",   0x11223346 }, // ????
#endif	/* NCI */
			{ "COM2", 0x11223347 },
			{ "COM3", 0x11223348 },
			{ "COM4", 0x11223349 },
#ifdef NCI
			{ "COM5", 0x11223350 },
			{ "COM6", 0x11223351 },
#endif	/* NCI */
#ifdef QNX
			{ "COM5", 0x11223350 },
			{ "COM6", 0x11223351 },
			{ "COM7", 0x11223352 },
			{ "COM8", 0x11223353 },
			{ "COM9", 0x11223354 },
			{ "COM10", 0x11223355 },
			{ "COM11", 0x11223356 },
			{ "COM12", 0x11223357 },
#endif /* QNX */
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
}	// GetSemID()
/*
 * Class:     javax_comm_CommPortIdentifier
 * Method:    monitorInterJVMDeviceAccessNC
 * Signature: (Ljava/lang/Thread;)I
 *
 * Currenty not Supported on Posix Devices
 */
int cygCommPortIdentifier_monitorInterJVMDeviceAccessNC
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
	int		semID;
	union semuni	scarg;
	int		mypid = getpid();
	int		scpid;
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
	semID = GetSemID(pnamec);
  	(*jenv)->ReleaseStringUTFChars(jenv, pname, pnamec);
	if (semID == -1)
		return -1;
	/* Get access to the interrupted method. */
	jthreadClass = (*jenv)->FindClass(jenv, "java/lang/Thread");
	assertexc(jthreadClass);
	jintMethod = (*jenv)->GetMethodID(jenv, jthreadClass, "isInterrupted", "()Z");
	assertexc(jintMethod);
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
#ifdef NCI
		if (oldVal) {
#ifdef  _POSIX_SEMAPHORES
			if(sem_wait(sem_lookup(semID)) <0){
#else
			if (semop(semID, dev_wait, NOOF_ELEMS(dev_wait)) < 0) {
#endif				
				(void)fprintf(stderr, "Java_javax_comm_CommPortIdentifier_monitorInterJVMDeviceAccessNC: semop error %d!\n", errno);
				return -1;
			}
		}
		else
#endif	/* NCI */
			sleep(pollingTime);
		/* Get the new value of the semaphore. */
			/* Get the current value of the semaphore. */
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
		oldVal = newVal;
	}	/* end of while() */
} /* Java_javax_comm_CommPortIdentifier_monitorInterJVMDeviceAccessNC */
