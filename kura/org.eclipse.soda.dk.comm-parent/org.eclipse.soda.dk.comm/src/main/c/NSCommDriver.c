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
#ifdef WIN32
#include "org_eclipse_soda_dk_comm_NSCommDriver.h"
extern  w32CommDriver_discoverDevicesNC(jenv, jobj);
#else
#if _WIN32_WCE>=400
#include "org_eclipse_soda_dk_comm_NSCommDriver.h"
extern  w32CommDriver_discoverDevicesNC(jenv, jobj);
#else
#include <org_eclipse_soda_dk_comm_NSCommDriver.h>
#endif
#endif
/*
 * Class:     com.ibm.comm.NSCommDriver
 * Method:    discoverDevicesNC
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_eclipse_soda_dk_comm_NSCommDriver_discoverDevicesNC
  (JNIEnv *jenv, jobject jobj) {
#ifdef WIN32
    w32CommDriver_discoverDevicesNC(jenv, jobj);
#else
#if _WIN32_WCE>=400
    w32CommDriver_discoverDevicesNC(jenv, jobj);
#else
    cygCommDriver_discoverDevicesNC(jenv, jobj);
#endif
#endif
    return;
}	// Java_org_eclipse_soda_dk_comm_NSCommDriver_discoverDevicesNC
