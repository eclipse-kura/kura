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
#ifdef WIN32
#include "org_eclipse_soda_dk_comm_NSDeviceOutputStream.h"
extern int w32DeviceOutputStream_writeDeviceNC(JNIEnv *, jobject, jbyteArray, jint, jint);
#else
#if _WIN32_WCE>=400
#include "org_eclipse_soda_dk_comm_NSDeviceOutputStream.h"
extern int w32DeviceOutputStream_writeDeviceNC(JNIEnv *, jobject, jbyteArray, jint, jint);
#else
#include <org_eclipse_soda_dk_comm_NSDeviceOutputStream.h>
#endif
#endif
/*
 * Class:     org_eclipse_soda_dk_comm_NSDeviceOutputStream
 * Method:    writeDeviceNC
 * Signature: ([BII)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_soda_dk_comm_NSDeviceOutputStream_writeDeviceNC
  (JNIEnv *jenv, jobject jobj, jbyteArray jbuf, jint off, jint len) {
#ifdef WIN32
    return w32DeviceOutputStream_writeDeviceNC(jenv, jobj, jbuf, off, len);
#else
#if _WIN32_WCE>=400
    return w32DeviceOutputStream_writeDeviceNC(jenv, jobj, jbuf, off, len);
#else
    return cygDeviceOutputStream_writeDeviceNC(jenv, jobj, jbuf, off, len);
#endif
#endif
}	/*/ Java_org_eclipse_soda_dk_comm_NSDeviceOutputStream_writeDeviceNC */
