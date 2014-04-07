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
#include <org_eclipse_soda_dk_comm_NSCommDriver.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>
#ifdef _POSIX_SEMAPHORES
#include <semaphore.h>
#include "SysVStyleSemaphore.h"
#else 
#include <sys/ipc.h> 
#include <sys/sem.h> 
#endif 
#include <stdlib.h>
#define assert(s) if (!s) {printf("\n\n%d asserted!\n\n", __LINE__); return;}
#define CREAT_PERMS	(0666)
// for unix only
// #define KERNEL_2200	"kernel.2200"
// #define KERNEL_1000	"kernel.1000"
typedef struct port_s {
	char		*portName;
	int		portType;
	char		*deviceName;
	int		semKey;
} port_t;
void cygCommDriver_discoverDevicesNC(JNIEnv *jenv, jobject jobj) {
  jclass	jc;
  jmethodID	jm;
  const int	PORT_SERIAL = 1;	// should match with CommPortIdentifier
  const int	PORT_PARALLEL = 2;	// should match with CommPortIdentifier
  struct stat	sbuf;
  jstring	pName;
  jstring	dName;
  jthrowable	jt;
  port_t	port_tbl[] =
		{
#ifdef NCI
			{ "LPT1", PORT_PARALLEL, "/dev/lpt0" , 0x11223344 },
			{ "COM1", PORT_SERIAL  , "/dev/tty00", 0x11223345 },
			{ "SC"  , PORT_SERIAL  , "/dev/sc"   , 0x11223346 }, // ????
			{ "COM2", PORT_SERIAL  , "/dev/tty01", 0x11223347 },
			{ "COM3", PORT_SERIAL  , "/dev/tty02", 0x11223348 },
			{ "COM4", PORT_SERIAL  , "/dev/tty03", 0x11223349 },
			{ "COM5", PORT_SERIAL  , "/dev/tty04", 0x11223350 },
			{ "COM6", PORT_SERIAL  , "/dev/tty05", 0x11223351 },
#endif	/* NCI */
#ifdef __linux__
			{ "LPT1", PORT_PARALLEL, "/dev/lp0"  , 0x11223344 },
			{ "COM1", PORT_SERIAL  , "/dev/ttyS0", 0x11223345 },
			{ "COM2", PORT_SERIAL  , "/dev/ttyS1", 0x11223347 },
			{ "COM3", PORT_SERIAL  , "/dev/ttyS2", 0x11223348 },
			{ "COM4", PORT_SERIAL  , "/dev/ttyS3", 0x11223349 },
#endif	/* __linux__ */
#ifdef QNX
			{ "LPT1", PORT_PARALLEL, "/dev/par1"  , 0x11223344 },
			{ "COM1", PORT_SERIAL  , "/dev/ser1", 0x11223345 },
			{ "COM2", PORT_SERIAL  , "/dev/ser2", 0x11223347 },
			{ "COM3", PORT_SERIAL  , "/dev/ser3", 0x11223348 },
			{ "COM4", PORT_SERIAL  , "/dev/ser4", 0x11223349 },
			{ "COM5", PORT_SERIAL  , "/dev/ser5", 0x11223350 },
			{ "COM6", PORT_SERIAL  , "/dev/ser6", 0x11223351 },
			{ "COM7", PORT_SERIAL  , "/dev/ser7", 0x11223352 },
			{ "COM8", PORT_SERIAL  , "/dev/ser8", 0x11223353 },
			{ "COM9", PORT_SERIAL  , "/dev/ser9", 0x11223354 },
			{ "COM10", PORT_SERIAL  , "/dev/ser10", 0x11223355 },
			{ "COM11", PORT_SERIAL  , "/dev/ser11", 0x11223356 },
			{ "COM12", PORT_SERIAL  , "/dev/ser12", 0x11223357 },
#endif	/* QNX  */
		};
  port_t	*pp;
  // Get access to the method to add a port.
  jc = (*jenv)->GetObjectClass(jenv, jobj);
  assert(jc);
  jm = (*jenv)->GetMethodID(jenv, jc, "addDeviceToList",
			    "(Ljava/lang/String;ILjava/lang/String;I)V");
  assert(jm);
  // Determine if we're running on a badger.  If so, use the alternate list.
  // For UNIX only
  //{
  // char  *envp =  getenv("BOOT_KERNEL");
  //
  // if (envp && *envp) {
  //    if (!strcmp(envp, KERNEL_2200)) {
  //       port_tbl = port_tbl_2200;
  //       port_tbl_noentries = sizeof(port_tbl_2200)/sizeof(port_tbl_2200[0]);
  //    }
  //    else if (!strcmp(envp, KERNEL_1000)) {
  //       port_tbl = port_tbl_1000;
  //       port_tbl_noentries = sizeof(port_tbl_1000)/sizeof(port_tbl_1000[0]);
  //    }
  // }
  //}
  // For all the pre-defined ports, check to see which ones exist, and add
  // them selectively.
  for (pp = port_tbl;
       pp < port_tbl+(sizeof(port_tbl)/sizeof(port_tbl[0]));
       ++pp) {
       if (stat(pp->deviceName, &sbuf) != -1) {
		int		semID;
		
		pName = (*jenv)->NewStringUTF(jenv, pp->portName);
		if (!pName)
			continue;
		dName = (*jenv)->NewStringUTF(jenv, pp->deviceName);
		if (!dName)
			continue;
		/* Obtain/create a semaphore for the device in consideration.
		   If it fails, don't lock/unlock it later on. */
#ifdef _POSIX_SEMAPHORES
		semID = sem_create(pp->semKey, 1);
#else
		semID = semget((key_t)pp->semKey, 1, IPC_CREAT | CREAT_PERMS);
#endif
#ifdef DEBUG
       printf( "%s ( %s ) semID %d\n", pName, dName, semID );
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
  }
  return;
}	// Java_org_eclipse_soda_dk_comm_NSCommDriver_discoverDevicesNC
