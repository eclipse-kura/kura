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
#include <fcntl.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/ipc.h>
#include <sys/sem.h>
#include <sys/ioctl.h>
#ifdef NCI
#include <dev/ic/lptioctl.h>
#include <dev/ic/lptreg.h>
#endif	/* NCI */
#ifdef __linux__
//#define __KERNEL__ /* To get the definitions for printer errors */
#include <linux/lp.h>
#endif	/* __linux__ */
#ifdef _POSIX_SEMAPHORES
#include <semaphore.h>
#include "SysVStyleSemaphore.h"
#else 
#include <sys/ipc.h> 
#include <sys/sem.h> 
#endif 
#include <org_eclipse_soda_dk_comm_NSParallelPort.h>
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
		{ 0, -1, (IPC_NOWAIT | SEM_UNDO) }	/* unlock it */
};
#endif
/*
 * Class:     org_eclipse_soda_dk_comm_NSParallelPort
 * Method:    closeDeviceNC
 * Signature: (II)I
 */
int cygParallelPort_closeDeviceNC
  (JNIEnv *jenv, jobject jobj, jint fd, jint semId)
{
  /* If the semaphore was locked, unlock it. */
  if (semId != -1) {
  /* fix debug for POSIX semaphores later */
#if defined(DEBUG) && !(_POSIX_SEMAPHORES)
    int iRet;
	 iRet = semop(semId, dev_unlock, NOOF_ELEMS(dev_unlock));
    printf( "Unlock semID %d return value: %d\n", semId, iRet );
     {
        union semuni scarg;
        int iVal = 0;
	     (void)memset(&scarg, 0, sizeof(scarg));
   	  iVal = semctl(semId, 0, GETVAL, scarg);
        printf( "semID %d value %d\n", semId, iVal );
        //fflush( stdout );
     }
#else
#ifndef _POSIX_SEMAPHORES
	(void)semop(semId, dev_unlock, NOOF_ELEMS(dev_unlock));
#else
 	 /* don't worry about return right now */
	 (void)sem_post(sem_lookup(semId));
#endif /* _POSIX_SEMAPHORES */
#endif
  }
  /* Drain any remaining data. */
#ifdef NCI
  (void)ioctl(fd, TIOCDRAIN, NULL);
#endif	/* NCI */
#ifdef __osx__
  (void)tcdrain(fd);
#endif	/* __osx__ */
#ifdef __linux__
  (void)tcdrain(fd);
#endif	/* __linux__ */
# ifdef QNX
  /*** BugBug: tcdrain hangs with parallel. Need to fix.
  (void)tcdrain(fd);
   ***/
#endif /* QNX */
  return close(fd);
}	/* cygParallelPort_closeDeviceNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSParallelPort
 * Method:    openDeviceNC
 * Signature: (Ljava/lang/String;I)I
 */
int cygParallelPort_openDeviceNC
  (JNIEnv *jenv, jobject jobj, jstring name, jint semId)
{
  const char * dname = NULL;
  int fd = -1;
  int sts;
  dname = (*jenv)->GetStringUTFChars(jenv, name, 0);
  if( dname == NULL )
  {
#ifdef DEBUG
    printf( "Can not open port because get NULL name\n" );
#endif
    return fd;
  }
  /* Test the semaphore to see if it is free. If so, lock it.  Else, return
     a failure. */
  if (semId != -1) {
#ifdef _POSIX_SEMAPHORES
   if(sem_trywait(sem_lookup(semId)) == -1){
#else 
	if (semop(semId, dev_test, NOOF_ELEMS(dev_test)) < 0 ||
	    semop(semId, dev_lock, NOOF_ELEMS(dev_lock)) < 0) {
#endif
#if defined(DEBUG) && !(_POSIX_SEMAPHORES)
     {
        union semuni scarg;
        int iVal = 0;
	     (void)memset(&scarg, 0, sizeof(scarg));
   	  iVal = semctl(semId, 0, GETVAL, scarg);
        printf( "semID %d was locked or can not be locked(value %d)\n", semId, iVal );
        /* fflush( stdout ); */
     }
#endif
		(*jenv)->ReleaseStringUTFChars(jenv, name, dname);
		return fd;
	}
  }
#ifdef DEBUG
  (void)fprintf(stderr, "Before opening %s\n", dname);
#endif /* DEBUG */
  fd = open( dname, O_RDWR | O_NONBLOCK );
#ifdef DEBUG
  (void)fprintf(stderr, "After opening %s; fd = %d\n", dname, fd);
#endif /* DEBUG */
  /* Turn on blocking mode for the device. */
  if (fd != -1) {
     if ((sts = fcntl(fd, F_GETFL, 0)) != -1) {
	sts &= ~O_NONBLOCK;
	(void)fcntl(fd, F_SETFL, sts);
     }
  }
  (*jenv)->ReleaseStringUTFChars(jenv, name, dname);
  /* If the open has failed and the semaphore was locked, unlock it. */
  if (fd == -1 && semId != -1) {
#ifdef _POSIX_SEMAPHORES
	(void) sem_post(sem_lookup(semId));
#else 
	(void)semop(semId, dev_unlock, NOOF_ELEMS(dev_unlock));
#endif
  }
  return fd;
}   /* win32ParallelPort_openDeviceNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSParallelPort
 * Method:    isPaperOutNC
 * Signature: (I)Z
 */
jboolean cygParallelPort_isPaperOutNC
  (JNIEnv *jenv, jobject jobj, jint jfd)
{
  int		status;
#ifdef NCI
  if (ioctl(jfd, PIOCSTATUS, &status) < 0) {
#endif	/* NCI */
#ifdef __osx__          /* ToDo: implement */
  if (0) {
#endif	/* __osx__ */
#ifdef __linux__
  if (ioctl(jfd, LPGETSTATUS, &status) < 0) {
#endif	/* __linux__ */
#ifdef QNX          /* ToDo: implement */
  if (0) {
#endif /* QNX*/	
     (void)fprintf(stderr, "Java_org_eclipse_soda_dk_comm_NSParallelPort_isPaperOutNC: ioctl error %d!\n", errno);
     return JNI_FALSE;
  }
#ifdef NCI
  return status & LPS_NOPAPER ? JNI_TRUE : JNI_FALSE;
#endif	/* NCI */
#ifdef __osx__          /* ToDo: implement */
  return JNI_FALSE;
#endif	/* __osx__ */
#ifdef __linux__
  return status & LP_POUTPA ? JNI_TRUE : JNI_FALSE;
#endif	/* __linux__ */
#ifdef QNX          /* ToDo: implement */
  return JNI_FALSE;
#endif /* QNX*/	
}	/* cygParallelPort_isPaperOutNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSParallelPort
 * Method:    isPrinterBusyNC
 * Signature: (I)Z
 */
jboolean cygParallelPort_isPrinterBusyNC
  (JNIEnv *jenv, jobject jobj, jint jfd)
{
  int		status;
#ifdef NCI
  if (ioctl(jfd, PIOCSTATUS, &status) < 0) {
#endif	/* NCI */
#ifdef __osx__          /* ToDo: implement */
  if (0) {
#endif	/* __osx__ */
#ifdef __linux__
  if (ioctl(jfd, LPGETSTATUS, &status) < 0) {
#endif	/* __linux__ */
#ifdef QNX          /* ToDo: implement */
  if (0) {
#endif /* QNX*/	
     (void)fprintf(stderr, "Java_org_eclipse_soda_dk_comm_NSParallelPort_isPrinterBusyNC: ioctl error %d!\n", errno);
     return JNI_FALSE;
  }
#ifdef NCI
  return status & LPS_NBSY ? JNI_FALSE : JNI_TRUE;	/* converse */
#endif	/* NCI */
#ifdef __osx__          /* ToDo: implement */
  return JNI_FALSE;
#endif	/* __osx__ */
#ifdef __linux__
  return status & LP_PBUSY ? JNI_FALSE : JNI_TRUE;	/* converse */
#endif	/* __linux__ */
#ifdef QNX          /* ToDo: implement */
  return JNI_FALSE;
#endif /* QNX*/	
}	/* cygParallelPort_isPrinterBusyNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSParallelPort
 * Method:    isPrinterSelectedNC
 * Signature: (I)Z
 */
jboolean cygParallelPort_isPrinterSelectedNC
  (JNIEnv *jenv, jobject jobj, jint jfd)
{
  int		status;
#ifdef NCI
  if (ioctl(jfd, PIOCSTATUS, &status) < 0) {
#endif	/* NCI */
#ifdef __osx__          /* ToDo: implement */
  if (0) {
#endif	/* __osx__ */
#ifdef __linux__
  if (ioctl(jfd, LPGETSTATUS, &status) < 0) {
#endif	/* __linux__ */
#ifdef QNX          /* ToDo: implement */
  if (0) {
#endif /* QNX*/	
     (void)fprintf(stderr, "Java_org_eclipse_soda_dk_comm_NSParallelPort_isPrinterSelectedNC: ioctl error %d!\n", errno);
     return JNI_FALSE;
  }
#ifdef NCI
  return status & LPS_SELECT ? JNI_FALSE : JNI_TRUE;	/* converse */
#endif	/* NCI */
#ifdef __osx__          /* ToDo: implement */
  return JNI_TRUE;
#endif	/* __osx__ */
#ifdef __linux__
  return status & LP_PSELECD ? JNI_FALSE : JNI_TRUE;	/* converse */
#endif	/* __linux__ */
#ifdef QNX          /* ToDo: implement */
  return JNI_TRUE;
#endif /* QNX*/	
}	/* cygParallelPort_isPrinterSelectedNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSParallelPort
 * Method:    isPrinterTimedOutNC
 * Signature: (I)Z
 */
jboolean cygParallelPort_isPrinterTimedOutNC
  (JNIEnv *jenv, jobject jobj, jint jfd)
{
  return JNI_FALSE;
}	/* cygParallelPort_isPrinterTimedOutNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSParallelPort
 * Method:    isPrinterErrorNC
 * Signature: (I)Z
 */
jboolean cygParallelPort_isPrinterErrorNC
  (JNIEnv *jenv, jobject jobj, jint jfd)
{
  int		status;
#ifdef NCI
  if (ioctl(jfd, PIOCSTATUS, &status) < 0) {
#endif	/* NCI */
#ifdef __osx__          /* ToDo: implement */
  if (0) {
#endif	/* __osx__ */
#ifdef __linux__
  if (ioctl(jfd, LPGETSTATUS, &status) < 0) {
#endif	/* __linux__ */
#ifdef QNX          /* ToDo: implement */
  if (0) {
#endif /* QNX*/	
     (void)fprintf(stderr, "Java_org_eclipse_soda_dk_comm_NSParallelPort_isPrinterErrorNC: ioctl error %d!\n", errno);
     return JNI_FALSE;
  }
#ifdef NCI
  return status & LPS_NERR ? JNI_FALSE : JNI_TRUE;	/* converse */
#endif	/* NCI */
#ifdef __osx__          /* ToDo: implement */
  return JNI_FALSE;
#endif	/* __osx__ */
#ifdef __linux__
  return status & LP_PERRORP ? JNI_FALSE : JNI_TRUE;	/* converse */
#endif	/* __linux__ */
#ifdef QNX          /* ToDo: implement */
  return JNI_FALSE;
#endif /* QNX*/	
}	/* cygParallelPort_isPrinterErrorNC */
