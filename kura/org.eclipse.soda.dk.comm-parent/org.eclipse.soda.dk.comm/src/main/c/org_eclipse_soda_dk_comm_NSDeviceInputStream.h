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
#include <jni.h>
/* Header for class org_eclipse_soda_dk_comm_NSDeviceInputStream */
#ifndef _Included_org_eclipse_soda_dk_comm_NSDeviceInputStream
#define _Included_org_eclipse_soda_dk_comm_NSDeviceInputStream
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     org_eclipse_soda_dk_comm_NSDeviceInputStream
 * Method:    readDeviceOneByteNC
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_soda_dk_comm_NSDeviceInputStream_readDeviceOneByteNC
  (JNIEnv *, jobject);
/*
 * Class:     org_eclipse_soda_dk_comm_NSDeviceInputStream
 * Method:    readDeviceNC
 * Signature: ([BII)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_soda_dk_comm_NSDeviceInputStream_readDeviceNC
  (JNIEnv *, jobject, jbyteArray, jint, jint);
/*
 * Class:     org_eclipse_soda_dk_comm_NSDeviceInputStream
 * Method:    getReadCountNC
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_soda_dk_comm_NSDeviceInputStream_getReadCountNC
  (JNIEnv *, jobject);
#ifdef __cplusplus
}
#endif
#endif
