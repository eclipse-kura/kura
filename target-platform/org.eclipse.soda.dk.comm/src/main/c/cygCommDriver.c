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
#if defined(__linux__) || defined(__osx__)
			{ "LPT1", PORT_PARALLEL, "/dev/lp0"  , 0x11223344 },
			{ "/dev/ttyS0", PORT_SERIAL  , "/dev/ttyS0", 0x11223345 },
			{ "/dev/ttyS1", PORT_SERIAL  , "/dev/ttyS1", 0x11223347 },
			{ "/dev/ttyS2", PORT_SERIAL  , "/dev/ttyS2", 0x11223348 },
			{ "/dev/ttyS3", PORT_SERIAL  , "/dev/ttyS3", 0x11223349 },
			{ "/dev/ttyS4", PORT_SERIAL  , "/dev/ttyS4", 0x11223350 },
			{ "/dev/ttyS5", PORT_SERIAL  , "/dev/ttyS5", 0x11223351 },
			{ "/dev/ttyS6", PORT_SERIAL  , "/dev/ttyS6", 0x11223352 },
			{ "/dev/ttyS7", PORT_SERIAL  , "/dev/ttyS7", 0x11223353 },
			{ "/dev/ttyS8", PORT_SERIAL  , "/dev/ttyS8", 0x11223354 },
			{ "/dev/ttyS9", PORT_SERIAL  , "/dev/ttyS9", 0x11223355 },
			{ "/dev/ttyO4", PORT_SERIAL  , "/dev/ttyO4", 0x11223356 },
			{ "/dev/ttyO5", PORT_SERIAL  , "/dev/ttyO5", 0x11223357 },
			{ "/dev/modem", PORT_SERIAL  , "/dev/modem", 0x11223358 },
			{ "/dev/ttyUSB0", PORT_SERIAL  , "/dev/ttyUSB0", 0x11223359 },
			{ "/dev/ttyUSB1", PORT_SERIAL  , "/dev/ttyUSB1", 0x11223360 },
			{ "/dev/ttyUSB2", PORT_SERIAL  , "/dev/ttyUSB2", 0x11223361 },
			{ "/dev/ttyUSB3", PORT_SERIAL  , "/dev/ttyUSB3", 0x11223362 },
			{ "/dev/ttyUSB4", PORT_SERIAL  , "/dev/ttyUSB4", 0x11223363 },
			{ "/dev/ttyUSB5", PORT_SERIAL  , "/dev/ttyUSB5", 0x11223364 },
			{ "/dev/ttyUSB6", PORT_SERIAL  , "/dev/ttyUSB6", 0x11223365 },
			{ "/dev/ttyUSB7", PORT_SERIAL  , "/dev/ttyUSB7", 0x11223366 },
			{ "/dev/ttyUSB8", PORT_SERIAL  , "/dev/ttyUSB8", 0x11223367 },
			{ "/dev/ttyUSB9", PORT_SERIAL  , "/dev/ttyUSB9", 0x11223368 },
			{ "/dev/ttyUSB10", PORT_SERIAL  , "/dev/ttyUSB10", 0x11223369 },
			{ "/dev/ttyUSB11", PORT_SERIAL  , "/dev/ttyUSB11", 0x11223370 },
			{ "/dev/ttyUSB12", PORT_SERIAL  , "/dev/ttyUSB12", 0x11223371 },
			{ "/dev/ttyUSB13", PORT_SERIAL  , "/dev/ttyUSB13", 0x11223372 },
			{ "/dev/ttyUSB14", PORT_SERIAL  , "/dev/ttyUSB14", 0x11223373 },
			{ "/dev/ttyUSB15", PORT_SERIAL  , "/dev/ttyUSB15", 0x11223374 },
			{ "/dev/ttyUSB16", PORT_SERIAL  , "/dev/ttyUSB16", 0x11223375 },
			{ "/dev/ttyUSB17", PORT_SERIAL  , "/dev/ttyUSB17", 0x11223376 },
			{ "/dev/ttyUSB18", PORT_SERIAL  , "/dev/ttyUSB18", 0x11223377 },
			{ "/dev/ttyUSB19", PORT_SERIAL  , "/dev/ttyUSB19", 0x11223378 },
			{ "/dev/ttyACM0", PORT_SERIAL  , "/dev/ttyACM0", 0x11223379 },
			{ "/dev/ttyACM1", PORT_SERIAL  , "/dev/ttyACM1", 0x11223380 },
			{ "/dev/ttyACM2", PORT_SERIAL  , "/dev/ttyACM2", 0x11223381 },
			{ "/dev/ttyACM3", PORT_SERIAL  , "/dev/ttyACM3", 0x11223382 },
			{ "/dev/ttyACM4", PORT_SERIAL  , "/dev/ttyACM4", 0x11223383 },
			{ "/dev/ttyACM5", PORT_SERIAL  , "/dev/ttyACM5", 0x11223384 },
			{ "/dev/ttyACM6", PORT_SERIAL  , "/dev/ttyACM6", 0x11223385 },
			{ "/dev/ttyACM7", PORT_SERIAL  , "/dev/ttyACM7", 0x11223386 },
			{ "/dev/ttyACM8", PORT_SERIAL  , "/dev/ttyACM8", 0x11223387 },
			{ "/dev/ttyACM9", PORT_SERIAL  , "/dev/ttyACM9", 0x11223388 },
			{ "/dev/ttyO0",   PORT_SERIAL  , "/dev/ttyO0", 0x11223389 },
			{ "/dev/ttyO1",   PORT_SERIAL  , "/dev/ttyO1", 0x11223390 },
			{ "/dev/ttyO2",   PORT_SERIAL  , "/dev/ttyO2", 0x11223391 },
			{ "/dev/ttyO3",   PORT_SERIAL  , "/dev/ttyO3", 0x11223392 },
			{ "/dev/ttyAMA0",    PORT_SERIAL  , "/dev/ttyAMA0", 0x11223393 },
			{ "/dev/tty.usbserial", PORT_SERIAL , "/dev/tty.usbserial", 0x11223394 },
			{ "/dev/ttymxc0",   PORT_SERIAL  , "/dev/ttymxc0", 0x11223395 },
			{ "/dev/ttymxc1",   PORT_SERIAL  , "/dev/ttymxc1", 0x11223396 },
			{ "/dev/ttymxc2",   PORT_SERIAL  , "/dev/ttymxc2", 0x11223397 },
			{ "/dev/ttymxc3",   PORT_SERIAL  , "/dev/ttymxc3", 0x11223398 },
			{ "/dev/ttymxc4",   PORT_SERIAL  , "/dev/ttymxc4", 0x11223399 },
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
