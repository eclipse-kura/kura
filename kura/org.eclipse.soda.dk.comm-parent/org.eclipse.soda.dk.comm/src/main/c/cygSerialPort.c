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
#include <fcntl.h>
#include <errno.h>
#include <unistd.h>
#ifdef QNX
 #include <sys/ioctl.h>
 #include <termios.h>
#endif
#ifdef NCI
 #include <termios.h>
#endif	/* NCI */
#ifdef __osx__
 #include <sys/ioctl.h>
 #include <termios.h>
#endif	/* __osx__ */
#ifdef __linux__
 //#include <asm/ioctls.h>
 #include <sys/ioctl.h>
 //#include <asm/termios.h>
 #include <termios.h>
 #endif	/* __linux__ */
#include <sys/types.h>
#ifdef _POSIX_SEMAPHORES
#include <semaphore.h>
#include "SysVStyleSemaphore.h"
#else 
#include <sys/ipc.h> 
#include <sys/sem.h> 
#endif 
#include <org_eclipse_soda_dk_comm_NSSerialPort.h>
#define assertexc(s) if (!s) {printf("\n\n%d asserted!\n\n", __LINE__); return(-1);}
#define NOOF_ELEMS(s)	((sizeof(s))/(sizeof(s[0])))
#define DEBUG
#ifndef _POSIX_SEMAPHORES
static struct sembuf	dev_test[] = {
		{ 0, 0, IPC_NOWAIT }	/* test to see if it is free */
};
static struct sembuf	dev_lock[] = {
		{ 0, 0, 0 },	/* wait for the semaphore to be free */
		{ 0, 1, SEM_UNDO }	/* lock it */
};
static struct sembuf	dev_unlock[] = {
		// { 0, -1, (IPC_NOWAIT | SEM_UNDO) }	/* unlock it */
		{ 0, -1,  0  }   	/* wait til unlock it */
};
#endif
static jint getfd(JNIEnv *jenv, jobject jobj)
  {
    jclass        jc;
    jfieldID      jf;
    jint          fd = -1;
    // Get the file descriptor.
    jc = (*jenv)->GetObjectClass(jenv, jobj);
    assertexc(jc);
    jf = (*jenv)->GetFieldID(jenv, jc, "fd", "I");
    assertexc(jf);
    fd = (*jenv)->GetIntField(jenv, jobj, jf);
    return fd;
  }
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    openDeviceNC
 * Signature: (Ljava/lang/String;I)I
 */
int cygSerialPort_closeDeviceNC
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
#if defined(__linux__) || defined(__osx__)
  (void)tcdrain(fd);
#endif	/* __linux__ */
# ifdef QNX
  (void)tcdrain(fd);
#endif /* QNX */
  return close(fd);
}	/* cygSerialPort_closeDeviceNC */
int cygSerialPort_openDeviceNC
  (JNIEnv *jenv, jobject jobj, jstring name, jint semId)
{
  sem_t * local_sem;
  const char * dname = NULL;
  int fd = -1;
  int sts;
  dname = (*jenv)->GetStringUTFChars(jenv, name, 0);
  if( dname == NULL )
  {

    printf( "Can not open port because get NULL name\n" );

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

  printf("Before opening %s\n", dname);

  fd = open( dname, O_RDWR | O_NONBLOCK );

  printf("After opening %s; fd = %d\n", dname, fd);

  /* Turn on blocking mode for the device. */
  if (fd != -1) {
     if ((sts = fcntl(fd, F_GETFL, 0)) != -1) {
		sts &= ~O_NONBLOCK;
		(void)fcntl(fd, F_SETFL, sts);
     }
  }
#define SMARTCARD
#ifdef SMARTCARD
  /* For SmartCard, set it to clocal, enable reader and turn off
     flow control. */
  if (fd != -1) {
	struct termios		io;
	if (tcgetattr(fd, &io) == -1)
			printf("tcgetattr() failed on %s(%d)!\n", dname, errno);
	else {
   		io.c_iflag &= ~(IXOFF | IXON | INLCR | ICRNL | IGNCR);
   		io.c_oflag &= ~(OPOST | OCRNL | ONLCR | ONOCR | ONLRET);
#ifdef NCI
   		io.c_cflag &= ~(CRTS_IFLOW | CCTS_OFLOW);
#endif	/* NCI */
#ifdef __linux__
   		io.c_cflag &= ~(CRTSCTS);
#endif	/* __linux__ */
#ifdef __osx__
		io.c_cflag &= ~(CRTSCTS);
#endif
#ifdef QNX
		io.c_cflag &=~(IHFLOW | OHFLOW);
#endif /* QNX */		
		io.c_cflag |= (CLOCAL | CREAD);
		io.c_lflag &= ~ICANON;	/* turn off cannonical processing */
		/* turn off echoing */
		io.c_lflag &= ~(ECHO | ECHOKE | ECHOE | ECHOCTL);
		io.c_lflag &= ~ISIG;
		io.c_cc[VMIN] = 1;
		io.c_cc[VTIME] = 0;
		if (tcsetattr(fd, TCSANOW, &io) == -1)
			printf("tcsetattr() failed on %s!\n", dname);
#if 0
		else
			printf("tcsetattr() set clocal, cread and no flow control on %s!\n", dname);
			/*fprintf(stderr, "tcsetattr() set clocal, cread and no flow control on %s!\n", dname); */
			
#endif	/* 0 */
	}
  }
#endif
  (*jenv)->ReleaseStringUTFChars(jenv, name, dname);
  /* If the open has failed and the semaphore was locked, unlock it. */
  if (fd == -1 && semId != -1) {
#ifdef _POSIX_SEMAPHORES
	(void)sem_post(sem_lookup(semId));
#else 
	(void)semop(semId, dev_unlock, NOOF_ELEMS(dev_unlock));
#endif  
  }
  return fd;
}    /* cygSerialPort_openDeviceNC */
int cygSerialPort_setFlowControlModeNC
  (JNIEnv *jenv, jobject jobj, jint fd, jint fc )
{
  int			fm = -1;
  int			rc;
  struct termios	ios;
  if ((rc = tcgetattr(fd, &ios)) == -1)
     return fm;
  /* Now set the desired flow control.  Turn off the other exclusive flow
     control mode.
   */
  /* In fact, set the flow control completely based on the flags just passed
     in.   That means, turn off all flow control modes that were set earlier.
   */
  ios.c_iflag &= ~(IXOFF | IXON);
#ifdef NCI
  ios.c_cflag &= ~(CRTS_IFLOW | CCTS_OFLOW);
#endif	/* NCI */
#if defined(__linux__) || defined(__osx__)
  ios.c_cflag &= ~(CRTSCTS);
#endif	/* __linux__ */
#ifdef QNX
  ios.c_cflag &=~(IHFLOW | OHFLOW);
#endif /* QNX */  
  if (fc == 0) {			// NONE
     /* Do nothing, as all the flow control modes are already turned off now. */
  }
  else {
     if (fc & 1) {		// RTSCTS_IN
	// ios.c_iflag &= ~IXOFF;
#ifdef NCI
	ios.c_cflag |= CRTS_IFLOW;
#endif	/* NCI */
#if defined(__linux__) || defined(__osx__)
	ios.c_cflag |= CRTSCTS;
#endif	/* __linux__ */
#ifdef QNX
	ios.c_cflag |=IHFLOW;
#endif /* QNX */	
     }
     if (fc & 2) {		// RTSCTS_OUT
	// ios.c_iflag &= ~IXON;
#ifdef NCI
	ios.c_cflag |= CCTS_OFLOW;
#endif	/* NCI */
#if defined(__linux__) || defined(__osx__)
	ios.c_cflag |= CRTSCTS;
#endif	/* __linux__ */
#ifdef QNX
	ios.c_cflag |= OHFLOW;
#endif /* QNX*/	
     }
     if (fc & 4) {		// XONXOFF_IN
	// ios.c_cflag &= ~CRTS_IFLOW;
	ios.c_iflag |= IXOFF;
     }
     if (fc & 8) {		// XONXOFF_OUT
	// ios.c_cflag &= ~CCTS_OFLOW;
	ios.c_iflag |= IXON;
     }
  }
  if ((rc = tcsetattr(fd, TCSANOW, &ios)) != -1)
     fm = fc;
  return fm;
}   /* cygSerialPort_setFlowControlModeNC */
int cygSerialPort_getFlowControlModeNC
  (JNIEnv *jenv, jobject jobj, jint fd )
{
  struct termios 	ios;
  int			rc;
  int			fm = -1;
  if ((rc = tcgetattr(fd, &ios)) == -1)
     return fm;
  // Determine the flow control.
#ifdef NCI 
  if ((!(ios.c_cflag & CRTS_IFLOW) && !(ios.c_cflag & CCTS_OFLOW)) &&
#endif	/* NCI */
#ifdef __osx__
  if ((!(ios.c_cflag & CRTSCTS)) &&
#endif	/* __osx__ */
#ifdef __linux__ 
  if ((!(ios.c_cflag & CRTSCTS)) &&
#endif	/* __linux__ */
#ifdef QNX
  if ((!(ios.c_cflag & IHFLOW) && !(ios.c_cflag & OHFLOW)) &&
#endif /* QNX */
      (!(ios.c_iflag & IXON) && !(ios.c_iflag & IXOFF)))
     fm = 0;				// NONE
  else {
     fm = 0;
#ifdef NCI
     if (ios.c_cflag & CRTS_IFLOW)	// RTSCTS_IN
#endif	/* NCI */
#ifdef __osx__
     if (ios.c_cflag & CRTSCTS)		// RTSCTS_IN
#endif	/* __osx__ */
#ifdef __linux__ 
     if (ios.c_cflag & CRTSCTS)		// RTSCTS_IN
#endif	/* __linux__ */
#ifdef QNX
     if (ios.c_cflag & IHFLOW)
#endif /* QNX */     
        fm |= 1;
#ifdef NCI
     if (ios.c_cflag & CCTS_OFLOW)	// RTSCTS_OUT
#endif	/* NCI */
#ifdef __osx__ 
     if (ios.c_cflag & CRTSCTS)		// RTSCTS_IN
#endif	/* __osx__ */
#ifdef __linux__ 
     if (ios.c_cflag & CRTSCTS)		// RTSCTS_IN
#endif	/* __linux__ */
#ifdef QNX
     if (ios.c_cflag & OHFLOW)
#endif /* QNX */     
        fm |= 2;
     if (ios.c_iflag & IXOFF)		// XONXOFF_IN
        fm |= 4;
     if (ios.c_iflag & IXON)		// XONXOFF_OUT
        fm |= 8;
  }
  
  return fm;
}   /* cygSerialPort_getFlowControlModeNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    getBaudRateNC
 * Signature: (I)I
 */
int cygSerialPort_getBaudRateNC
  (JNIEnv *jenv, jobject jobj, jint fd)
{
  struct termios ios;
  int rc = -1;
  speed_t sp;
#ifdef DEBUG
  printf( "getBaudRateNC\n" );
  /* fflush( stdout ); */
#endif
 (void)memset(&ios, 0, sizeof(ios));
#ifdef DEBUG
  printf( "tcgetattr()\n" );
  /* fflush( stdout ); */
#endif
 rc = tcgetattr(fd, &ios);
 if ( rc ==  -1 ) return rc;
#ifdef DEBUG
  printf( "cfgetospeed()\n" );
  /* fflush( stdout ); */
#endif
 sp = cfgetospeed( &ios );
#if defined(__linux__) || defined(__osx__)
 /* Map the internal value to external speed. */
#ifdef DEBUG
  printf( "sp %d\n",sp);
  /*fflush( stdout ); */
#endif
 switch((int)sp) {
    case B50:		sp = 50; break;
    case B75:		sp = 75; break;
    case B110:		sp = 110; break;
    case B134:		sp = 134; break;
    case B150:		sp = 150; break;
    case B200:		sp = 200; break;
    case B300:		sp = 300; break;
    case B600:		sp = 600; break;
    case B1200:		sp = 1200; break;
    case B1800:		sp = 1800; break;
    case B2400:		sp = 2400; break;
    case B4800:		sp = 4800; break;
    case B9600:		sp = 9600; break;
    case B19200:	sp = 19200; break;
    case B38400:	sp = 38400; break;
    case B57600:	sp = 57600; break;
    case B115200:	sp = 115200; break;
    case B230400:	sp = 230400; break;
 }
#ifdef DEBUG
  printf( " new sp %d\n", sp);
  /* fflush( stdout ); */
#endif
#endif	/* __linux__ */
 return (jint) sp;
}   /* cygSerialPort_getFlowControlModeNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    getDataBitsNC
 * Signature: (I)I
 */
int cygSerialPort_getDataBitsNC(JNIEnv *jenv, jobject jobj, jint fd)
{
  struct termios ios;
  int rc = -1;
  rc = tcgetattr(fd, &ios);
  if ( rc ==  -1 ) return (jint) rc;
  rc = ios.c_cflag & CSIZE;
  switch ( rc )
  {
    case CS5:
      return (jint) 5;
    break;
    case CS6:
      return (jint) 6;
    break;
    case CS7:
      return (jint) 7;
    break;
    case CS8:
      return (jint) 8;
    break;
    default:
      return (jint) -1;
  }
  return (jint) rc;
} /* cygSerialPort_getDataBitsNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    getStopBitsNC
 * Signature: (I)I
 */
int cygSerialPort_getStopBitsNC(JNIEnv *jenv, jobject jobj, jint fd)
{
  struct termios ios;
  int rc = -1;
  rc = tcgetattr(fd, &ios);
  if ( rc ==  -1 ) return (jint) rc;
  if ( ios.c_cflag & CSTOPB )
    rc = 2;
  else
    rc = 1;
  return (jint) rc;
}  /* cygSerialPort_getStopBitsNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    getParityNC
 * Signature: (I)I
 */
int cygSerialPort_getParityNC(JNIEnv *jenv, jobject jobj, jint fd)
{
  struct termios ios;
  int rc = -1;
  int NONE = 0, ODD = 1, EVEN = 2;
  rc = tcgetattr(fd, &ios);
  if ( rc ==  -1 ) return (jint) rc;
  if ( ios.c_cflag & PARENB )
  {
    if ( ios.c_cflag & PARODD )
      rc = ODD;
    else
      rc = EVEN;
  } else
    rc = NONE; // PARITY_NONE
#ifdef DEBUG
  /*fprintf(stderr, "getParityNC:  parity = %d\n", rc); */
#endif /* DEBUG */
  return (jint) rc;
}  /* cygSerialPort_getParityNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    setDTRNC
 * Signature: (Z)V
 */
void cygSerialPort_setDTRNC(JNIEnv *jenv, jobject jobj, jint fd, jboolean bool)
{
    int jdtr;
    int rc = -1;
    fd = -1;
    fd = getfd( jenv, jobj );
    rc = ioctl( fd, TIOCMGET, &jdtr );
    if ( rc ==  -1 ) return;
    if ( bool == JNI_TRUE )
      jdtr |= TIOCM_DTR;
    else
      jdtr &= ~TIOCM_DTR;
    rc = ioctl( fd, TIOCMSET, &jdtr );
}  /* cygSerialPort_setDTRNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    isDTRNC
 * Signature: ()Z
 */
jboolean cygSerialPort_isDTRNC(JNIEnv *jenv, jobject jobj)
{
    int jdtr;
    int rc = -1;
    jint fd = -1;
    fd = getfd( jenv, jobj );
    rc = ioctl( fd, TIOCMGET, &jdtr );
    if ( rc ==  -1 ) return (jboolean)JNI_FALSE;
    if ( jdtr & TIOCM_DTR )
      return (jboolean)JNI_TRUE;
    else
      return (jboolean)JNI_FALSE;
}  /* cygSerialPort_isDTRNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    setRTSNC
 * Signature: (Z)V
 */
void cygSerialPort_setRTSNC(JNIEnv *jenv, jobject jobj, jboolean bool)
{
    int jrts;
    int rc = -1;
    jint fd = -1;
    fd = getfd( jenv, jobj );
    rc = ioctl( fd, TIOCMGET, &jrts );
    if ( rc ==  -1 ) return;
    if ( bool == JNI_TRUE )
      jrts |= TIOCM_RTS;
    else
      jrts &= ~TIOCM_RTS;
    rc = ioctl( fd, TIOCMSET, &jrts );
}  /* cygSerialPort_setRTSNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    isRTSNC
 * Signature: ()Z
 */
jboolean cygSerialPort_isRTSNC(JNIEnv *jenv, jobject jobj)
{
    int jrts;
    int rc = -1;
    jint fd = -1;
    fd = getfd( jenv, jobj );
    rc = ioctl( fd, TIOCMGET, &jrts );
    if ( rc ==  -1 ) return (jboolean) JNI_FALSE;
    if ( jrts & TIOCM_RTS )
      return (jboolean) JNI_TRUE;
    else
      return (jboolean) JNI_FALSE;
}  /* cygSerialPort_isRTSNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    isCTSNC
 * Signature: ()Z
 */
jboolean cygSerialPort_isCTSNC(JNIEnv *jenv, jobject jobj)
{
    int jcts;
    int rc = -1;
    jint fd = -1;
    fd = getfd( jenv, jobj );
    rc = ioctl( fd, TIOCMGET, &jcts );
    if ( rc ==  -1 ) return (jboolean) JNI_FALSE;
    if ( jcts & TIOCM_CTS )
      return (jboolean) JNI_TRUE;
    else
      return (jboolean) JNI_FALSE;
}  /* cygSerialPort_isCTSNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    isDSRNC
 * Signature: ()Z
 */
jboolean cygSerialPort_isDSRNC(JNIEnv *jenv, jobject jobj)
{
    int jdsr;
    int rc = -1;
    jint fd = -1;
    fd = getfd( jenv, jobj );
    rc = ioctl( fd, TIOCMGET, &jdsr );
    if ( rc ==  -1 ) return (jboolean) JNI_FALSE;
    if ( jdsr & TIOCM_CTS )
      return (jboolean) JNI_TRUE;
    else
      return (jboolean) JNI_FALSE;
}  /* cygSerialPort_isDSRNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    isRINC
 * Signature: ()Z
 */
jboolean cygSerialPort_isRINC(JNIEnv *jenv, jobject jobj)
{
    int jrng;
    int rc = -1;
    jint fd = -1;
    fd = getfd( jenv, jobj );
    rc = ioctl( fd, TIOCMGET, &jrng );
    if ( rc ==  -1 ) return (jboolean) JNI_FALSE;
    if ( jrng & TIOCM_RNG )
      return (jboolean) JNI_TRUE;
    else
      return (jboolean) JNI_FALSE;
}  /* cygSerialPort_isRINC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    isCDNC
 * Signature: ()Z
 */
jboolean cygSerialPort_isCDNC(JNIEnv *jenv, jobject jobj)
{
    int jcd;
    int rc = -1;
    jint fd = -1;
    fd = getfd( jenv, jobj );
    rc = ioctl( fd, TIOCMGET, &jcd );
    if ( rc ==  -1 ) return (jboolean) JNI_FALSE;
    if ( jcd & TIOCM_CD )
      return (jboolean) JNI_TRUE;
    else
      return (jboolean) JNI_FALSE;
}  /* cygSerialPort_isCDNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    sendBreakNC
 * Signature: (II)I
 */
int cygSerialPort_sendBreakNC(JNIEnv *jenv, jobject jobj, jint jfd, jint jmillis) {
   (void)tcsendbreak(jfd, jmillis);
} /* Java_org_eclipse_soda_dk_comm_NSSerialPort_sendBreakNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    setSerialPortParamsNC
 * Signature: (IIIII)I
 */
int cygSerialPort_setSerialPortParamsNC
  (JNIEnv *jenv, jobject jobj, jint jfd, jint jbd, jint jdb, jint jsb, jint jpar) {
  int			rc;
  struct termios 	ios;
  speed_t		spd;
  if ((rc = tcgetattr(jfd, &ios)) == -1)
     return rc;
  // Set the baud rate.
  spd = jbd;
#ifdef QNX  /* put in by daniel */
   if ((rc = cfsetospeed(&ios, spd)) == -1)
     return rc;
	if ((rc = cfsetispeed(&ios, spd)) == -1)
     return rc;
#else
  if ((rc = cfsetspeed(&ios, spd)) == -1)
     return rc;
#endif 
  // Set the data bits.
  switch(jdb) {
     case 5:		// DATABITS_5
	ios.c_cflag &= ~CSIZE;
	ios.c_cflag |= CS5;
	break;
     case 6:		// DATABITS_6
	ios.c_cflag &= ~CSIZE;
	ios.c_cflag |= CS6;
	break;
     case 7:		// DATABITS_7
	ios.c_cflag &= ~CSIZE;
	ios.c_cflag |= CS7;
	break;
     case 8:		// DATABITS_8
	ios.c_cflag &= ~CSIZE;
	ios.c_cflag |= CS8;
	break;
  }
  // Set the stop bits. 1.5 is not supported.
  switch (jsb) {
     case 1:		// STOPBITS_1
	ios.c_cflag &= ~CSTOPB;
	break;
     case 2:		// STOPBITS_2
	ios.c_cflag |= CSTOPB;
	break;
  }
  // Set the parity.  MARK and SPACE are not supported.
  switch (jpar) {
     case 0:		// PARITY_NONE
#ifdef DEBUG
	/* fprintf(stderr, "setSerialPortParamsNC: parity set to %s\n", "NONE"); */
#endif /* DEBUG */
	ios.c_cflag &= ~PARENB;
	break;
     case 1:		// PARITY_ODD
#ifdef DEBUG
	/* fprintf(stderr, "setSerialPortParamsNC: parity set to %s\n", "ODD"); */
#endif /* DEBUG */
	ios.c_cflag |= PARENB;
	ios.c_cflag |= PARODD;
	break;
     case 2:		// PARITY_EVEN
#ifdef DEBUG
	/* fprintf(stderr, "setSerialPortParamsNC: parity set to %s\n", "EVEN"); */
#endif /* DEBUG */
	ios.c_cflag |= PARENB;
	ios.c_cflag &= ~PARODD;
  }
  // Now set the desired communication characteristics.
  rc = tcsetattr(jfd, TCSANOW, &ios);
  return rc;
} /* cygSerialPort_setSerialPortParamsNC */
