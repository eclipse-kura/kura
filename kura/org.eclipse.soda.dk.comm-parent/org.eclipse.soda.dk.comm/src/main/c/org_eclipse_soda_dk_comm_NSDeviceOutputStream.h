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
/* Header for class org_eclipse_soda_dk_comm_NSDeviceOutputStream */
#ifndef _Included_org_eclipse_soda_dk_comm_NSDeviceOutputStream
#define _Included_org_eclipse_soda_dk_comm_NSDeviceOutputStream
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     org_eclipse_soda_dk_comm_NSDeviceOutputStream
 * Method:    writeDeviceNC
 * Signature: ([BII)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_soda_dk_comm_NSDeviceOutputStream_writeDeviceNC
  (JNIEnv *, jobject, jbyteArray, jint, jint);
#ifdef __cplusplus
}
#endif
#endif
