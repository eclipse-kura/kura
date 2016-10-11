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
#include "org_eclipse_soda_dk_comm_NSDeviceInputStream.h"
extern int w32DeviceInputStream_readDeviceOneByteNC(JNIEnv *, jobject);
extern int w32DeviceInputStream_readDeviceNC(JNIEnv *, jobject, jbyteArray, jint, jint);
extern int w32DeviceInputStream_getReadCountNC(JNIEnv *, jobject);
#else
#if _WIN32_WCE>=400
#include "org_eclipse_soda_dk_comm_NSDeviceInputStream.h"
extern int w32DeviceInputStream_readDeviceOneByteNC(JNIEnv *, jobject);
extern int w32DeviceInputStream_readDeviceNC(JNIEnv *, jobject, jbyteArray, jint, jint);
extern int w32DeviceInputStream_getReadCountNC(JNIEnv *, jobject);
#else
#include <org_eclipse_soda_dk_comm_NSDeviceInputStream.h>
#endif
#endif
/*
 * Class:     org_eclipse_soda_dk_comm_NSDeviceInputStream
 * Method:    readDeviceOneByteNC
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_soda_dk_comm_NSDeviceInputStream_readDeviceOneByteNC
  (JNIEnv *jenv, jobject jobj) {
#ifdef WIN32
	return /*(int)(unsigned char)*/w32DeviceInputStream_readDeviceOneByteNC(jenv, jobj);
#else
#if _WIN32_WCE>=400
	return (int)(unsigned char)w32DeviceInputStream_readDeviceOneByteNC(jenv, jobj);
#else
    return (int)(unsigned char)cygDeviceInputStream_readDeviceOneByteNC(jenv, jobj);
#endif
#endif
}	/* cygDeviceInputStream_readDeviceOneByteNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSDeviceInputStream
 * Method:    readDeviceNC
 * Signature: ([BII)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_soda_dk_comm_NSDeviceInputStream_readDeviceNC
  (JNIEnv *jenv, jobject jobj, jbyteArray jba, jint off, jint len) {
#ifdef WIN32
	return w32DeviceInputStream_readDeviceNC(jenv, jobj, jba, off, len);
#else
#if _WIN32_WCE>=400
	return w32DeviceInputStream_readDeviceNC(jenv, jobj, jba, off, len);
#else
    return cygDeviceInputStream_readDeviceNC(jenv, jobj, jba, off, len);
#endif
#endif
}	/* cygDeviceInputStream_readDeviceNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSDeviceInputStream
 * Method:    getReadCountNC
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_soda_dk_comm_NSDeviceInputStream_getReadCountNC
  (JNIEnv *jenv, jobject jobj) {
#ifdef WIN32
  return w32DeviceInputStream_getReadCountNC(jenv, jobj);
#else
#if _WIN32_WCE>=400
  return w32DeviceInputStream_getReadCountNC(jenv, jobj);
#else
  return cygDeviceInputStream_getReadCountNC(jenv, jobj);
#endif
#endif
}
