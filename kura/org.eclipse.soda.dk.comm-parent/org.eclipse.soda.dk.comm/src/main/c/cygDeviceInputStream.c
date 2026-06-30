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
#include <stdlib.h>
#include <string.h>
#include <signal.h>
#include <errno.h>
#include <sys/ioctl.h>
#include <org_eclipse_soda_dk_comm_NSDeviceInputStream.h>
#include <sys/time.h>
#ifndef FALSE
#define FALSE 0
#endif
#ifndef TRUE
#define TRUE 1
#endif
#define assert(s) if (!s) {printf("\n\n%d asserted!\n\n", __LINE__); return(-1);}
#define assertexc(s) if (!s) {printf("\n\n%d asserted!\n\n", __LINE__); (*jenv)->ThrowNew(jenv, ec, "");}
#if defined(__linux__) || defined(__osx__)
static volatile int	timeoutOccurred = FALSE;
static struct itimerval	apptimer;
static struct sigaction	appsa;
// static void		(*apphandler)();
int			timerflag;
static void alarm_handler(int sig)
{
	 timeoutOccurred = TRUE; 
}
static void start_read_timer(int tmovalue)
{
   struct itimerval	tm;
   struct sigaction	newsa;
   /* Suspend the app's timer and save it. */
   (void)memset(&tm, 0, sizeof(tm));
   (void)setitimer(ITIMER_REAL, &tm, &apptimer);
   /* Replace the app's signal handler with ours. */
   (void)memset(&newsa, 0, sizeof(newsa));
   newsa.sa_handler = alarm_handler;
   (void)sigaction(SIGALRM, &newsa, &appsa);
   // apphandler = signal(SIGALRM, alarm_handler);
   /* Start our timer. */
   (void)memset(&tm, 0, sizeof(tm));
   tm.it_value.tv_usec = tmovalue * 1000;  // convert millisec into microsec
   (void)setitimer(ITIMER_REAL, &tm, NULL);
   timerflag = TRUE;
}	/* start_read_timer() */
static void stop_read_timer()
{
   struct itimerval	tm;
   if (!timerflag)
      return;
   /* Stop our ticking timer. */
   (void)memset(&tm, 0, sizeof(tm));
   (void)setitimer(ITIMER_REAL, &tm, NULL);
   /* Restore the app's signal handler. */
   (void)sigaction(SIGALRM, &appsa, NULL);
   // (void)signal(SIGALRM, apphandler);
   /* Restore the app's timer */
   (void)setitimer(ITIMER_REAL, &apptimer, NULL);
   timerflag = FALSE;
}	/* stop_read_timer */
#endif /* __linux__ */
int cygDeviceInputStream_readDeviceOneByteNC
  (JNIEnv *jenv, jobject jobj) {
  jclass	jc;
  jclass	ec;
  jfieldID	jf;
  jint 		fd = -1;
  int		rc;
  int		dc;
  char		buf[1];
  jfieldID	tmof;
  int		tmo;
  int		serrno;
	
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
  tmof = (*jenv)->GetFieldID(jenv, jc, "tmo", "I");
  assert(tmof);
  tmo = (*jenv)->GetIntField(jenv, jobj, tmof);
  
#ifdef QNX
	// Read data - QNX with timeout
	if (tmo <100 & tmo>0) {
		tmo=100;
	}
	dc=readcond(fd, buf, 1, 1, 0, tmo/100); //10th of a second instead of microSecs
	if (dc<1) {
		//dc=-1; //return fake error
	}
#endif /* QNX */
#if defined(__linux__) || defined(__osx__)
  // Start the timer.
  timeoutOccurred = FALSE;
  if (tmo > 0)
	start_read_timer(tmo);
  // Read data.
  buf[0] = 0;
  dc = read(fd, buf, 1);
  serrno = errno;
  // Stop the timer.
  if (tmo > 0)
	stop_read_timer();
  if (dc < 0 && !(serrno == EINTR || serrno == EAGAIN)) {
	(*jenv)->ThrowNew(jenv, ec, "");
  }
#endif /* __linux__ */
  // If timeout had occurred, or if nil data was received, return -1.
  if (dc <= 0)
	return -1;
  return (int)(unsigned char)buf[0];
}	/* cygDeviceInputStream_readDeviceOneByteNC */
int cygDeviceInputStream_readDeviceNC
  (JNIEnv *jenv, jobject jobj, jbyteArray jba, jint off, jint len) {
  jclass	jc;
  jfieldID	jf;
  jint 		fd = -1;
  int		rc;
  int		dc = 0;
  char		*cbuf;
  jfieldID	tmof;
  int		tmo;
  jfieldID	tmoDonef;
  
  cbuf = malloc(len);
  
  assert(cbuf);
  // Get the file descriptor.
  jc = (*jenv)->GetObjectClass(jenv, jobj);
  assert(jc);
  jf = (*jenv)->GetFieldID(jenv, jc, "fd", "I");
  assert(jf);
  fd = (*jenv)->GetIntField(jenv, jobj, jf);
  if (fd == -1) {
	return -1;
  }
  tmof = (*jenv)->GetFieldID(jenv, jc, "tmo", "I");
  assert(tmof);
  tmoDonef = (*jenv)->GetFieldID(jenv, jc, "tmoDone", "Z");
  assert(tmoDonef);
  tmo = (*jenv)->GetIntField(jenv, jobj, tmof);
#ifdef QNX
	// Read data - QNX with timeout
	if (tmo <100 & tmo>0) {
		tmo=100;
	}
	
	dc=readcond(fd, cbuf, len, len, tmo/100, tmo/100); //10th of a second instead of microSecs
	if (dc<len) {
		(*jenv)->SetBooleanField(jenv, jobj, tmoDonef, (jboolean)JNI_TRUE);
		//dc=-1; //return fake error
	}
#endif /* QNX */
#if defined(__linux__) || defined(__osx__) 
  // Start the timer.
  timeoutOccurred = FALSE;
  if (tmo > 0)
	start_read_timer(tmo);
  
    //Read data
   dc = read(fd, cbuf, len);
	
  // If the timer had already expired, set the field tmoDone.
  // Else, stop the timer.
    if (timeoutOccurred) {
    	dc = 0; // Bug fix for PR#117959
		(*jenv)->SetBooleanField(jenv, jobj, tmoDonef, (jboolean)JNI_TRUE);
  	}
  if (tmo > 0)
    stop_read_timer();
#endif /*__linux__*/
  // Copy back the data into the java buffer.
  if (dc > 0)
	(*jenv)->SetByteArrayRegion(jenv, jba, off, dc, (jbyte*)cbuf);
  free(cbuf);
  return dc;
}	/* cygDeviceInputStream_readDeviceNC */
int cygDeviceInputStream_getReadCountNC
  (JNIEnv *jenv, jobject jobj) {
  jclass	jc;
  jclass	ec;
  jfieldID	jf;
  jint 		fd = -1;
  int		rc;
  int		dc = 0;
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
  // Now query the device stream for any data to be read.
  if ((rc = ioctl(fd, FIONREAD, &dc)) == -1)
	(*jenv)->ThrowNew(jenv, ec, "");
  return dc;
} /* cygDeviceInputStream_getReadCountNC */
