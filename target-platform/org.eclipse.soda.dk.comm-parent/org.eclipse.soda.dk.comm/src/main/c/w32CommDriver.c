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
#ifndef _WIN32_WCE
#include <sys/stat.h>
#include <sys/types.h>
#endif
#include "org_eclipse_soda_dk_comm_NSCommDriver.h"
#include "NSCommLOG.h"
#if 0 //t.j
#include <unistd.h>
#ifdef _POSIX_SEMAPHORES
#include <semaphore.h>
#include "SysVStyleSemaphore.h"
#else 
#include <sys/ipc.h> 
#include <sys/sem.h> 
#endif 
#endif //t.j
#define assert(s)       if (!s) {printf("\n\n%d asserted!\n\n", __LINE__); \
				 return;}
#define CREAT_PERMS	(0666)
typedef struct port_s {
	char	*portName;
	int		portType;
	char	*deviceName;
	int		semKey;
} port_t;
void w32CommDriver_discoverDevicesNC(JNIEnv *jenv, jobject jobj) {
	jclass	jc;
	jmethodID	jm;
	const int	PORT_SERIAL   = 1;	// should match with CommPortIdentifier
	const int	PORT_PARALLEL = 2;	// should match with CommPortIdentifier
//	struct stat	sbuf;
	jstring		pName, dName=NULL;
	jthrowable	jt;
	int			semID=0x11223344;
  port_t	port_tbl[] =
		{
			{ "LPT1", PORT_PARALLEL, "LPT1", 0x11223344 },
			{ "COM1", PORT_SERIAL  , "COM1", 0x11223345 },
			{ "COM2", PORT_SERIAL  , "COM2", 0x11223347 },
			{ "COM3", PORT_SERIAL  , "COM3", 0x11223348 },
			{ "COM4", PORT_SERIAL  , "COM4", 0x11223349 },
			{ "COM5", PORT_SERIAL  , "COM5", 0x11223350 },
			{ "COM6", PORT_SERIAL  , "COM6", 0x11223351 },
			{ "COM7", PORT_SERIAL  , "COM7", 0x11223352 },
			{ "COM8", PORT_SERIAL  , "COM8", 0x11223353 },
			{ "COM9", PORT_SERIAL  , "COM9", 0x11223354 },
			{ "COM10", PORT_SERIAL  , "\\\\.\\COM10", 0x11223355 },
			{ "COM11", PORT_SERIAL  , "\\\\.\\COM11", 0x11223356 },
			{ "COM12", PORT_SERIAL  , "\\\\.\\COM12", 0x11223357 },
			{ "COM13", PORT_SERIAL  , "\\\\.\\COM13", 0x11223358 },
			{ "COM14", PORT_SERIAL  , "\\\\.\\COM14", 0x11223359 },
			{ "COM15", PORT_SERIAL  , "\\\\.\\COM15", 0x11223360 },
			{ "COM16", PORT_SERIAL  , "\\\\.\\COM16", 0x11223361 },
			{ "COM17", PORT_SERIAL  , "\\\\.\\COM17", 0x11223362 },
			{ "COM18", PORT_SERIAL  , "\\\\.\\COM18", 0x11223363 },
			{ "COM19", PORT_SERIAL  , "\\\\.\\COM19", 0x11223364 },
			{ "COM20", PORT_SERIAL  , "\\\\.\\COM20", 0x11223365 },
		};
  port_t	*pp;
  // Get access to the method to add a port.
  jc = (*jenv)->GetObjectClass(jenv, jobj);
  assert(jc);
  jm = (*jenv)->GetMethodID(jenv, jc, "addDeviceToList",
			    "(Ljava/lang/String;ILjava/lang/String;I)V");
  assert(jm);
  // For all the pre-defined ports, check to see which ones exist, and add
  // them selectively.
  for (pp = port_tbl; pp < port_tbl+(sizeof(port_tbl)/sizeof(port_tbl[0])); ++pp) {
	pName = (*jenv)->NewStringUTF(jenv, pp->portName);
	if (!pName) continue;
    
	dName = (*jenv)->NewStringUTF(jenv, pp->deviceName);
	if (!dName) continue;
#if 0 //t.j
		/* Obtain/create a semaphore for the device in consideration.
		   If it fails, don't lock/unlock it later on. */
#ifdef _POSIX_SEMAPHORES
		semID = sem_create(pp->semKey, 1);
#else
		semID = semget((key_t)pp->semKey, 1, IPC_CREAT | CREAT_PERMS);
#endif
#endif
#ifdef DEBUG
//       LOG( ("pName: %s, %s, dName: %s, semID %d\n", pName,pp->portName, dName, semID) );
       printf( "pName: %s %s, dNama: %s semID %d\n", pName, pp->portName, dName, semID );
       fflush( stdout );
#endif
		
	 (*jenv)->CallVoidMethod(jenv, jobj, jm,
					pName, pp->portType, dName, semID);
	 jt = (*jenv)->ExceptionOccurred(jenv);
	 if (jt) {
	   	(*jenv)->ExceptionDescribe(jenv);
	   	(*jenv)->ExceptionClear(jenv);
	 }
  }
  return;
}	/* Java_org_eclipse_soda_dk_comm_NSCommDriver_discoverDevicesNC */
