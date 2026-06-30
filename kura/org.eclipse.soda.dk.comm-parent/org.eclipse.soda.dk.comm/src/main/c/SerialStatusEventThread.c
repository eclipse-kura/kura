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
#include "org_eclipse_soda_dk_comm_SerialStatusEventThread.h"
extern void w32SerialStatusEventThread_monitorSerialStatusNC(JNIEnv *, jobject, jint);
#else
#if _WIN32_WCE>=400
#include "org_eclipse_soda_dk_comm_SerialStatusEventThread.h"
extern void w32SerialStatusEventThread_monitorSerialStatusNC(JNIEnv *, jobject, jint);
#else
#include <org_eclipse_soda_dk_comm_SerialStatusEventThread.h>
#endif
#endif
/*
 * Class:     org_eclipse_soda_dk_comm_SerialStatusEventThread
 * Method:    monitorSerialStatusNC
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_org_eclipse_soda_dk_comm_SerialStatusEventThread_monitorSerialStatusNC
(JNIEnv *jenv, jobject jobj, jint jfd) {
#ifdef WIN32
	w32SerialStatusEventThread_monitorSerialStatusNC(jenv, jobj, jfd);
#else
#if _WIN32_WCE>=400
	w32SerialStatusEventThread_monitorSerialStatusNC(jenv, jobj, jfd);
#else
    cygSerialStatusEventThread_monitorSerialStatusNC(jenv, jobj, jfd);
#endif
#endif
} /* Java_org_eclipse_soda_dk_comm_SerialStatusEventThread_monitorSerialStatusNC */
