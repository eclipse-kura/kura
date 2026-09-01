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
/* Header for class javax_comm_CommPortIdentifier */
#ifndef _Included_javax_comm_CommPortIdentifier
#define _Included_javax_comm_CommPortIdentifier
#ifdef __cplusplus
extern "C" {
#endif
#undef javax_comm_CommPortIdentifier_pollingTime
#define javax_comm_CommPortIdentifier_pollingTime 1L
#undef javax_comm_CommPortIdentifier_PORT_SERIAL
#define javax_comm_CommPortIdentifier_PORT_SERIAL 1L
#undef javax_comm_CommPortIdentifier_PORT_PARALLEL
#define javax_comm_CommPortIdentifier_PORT_PARALLEL 2L
/*
 * Class:     javax_comm_CommPortIdentifier
 * Method:    monitorInterVMDeviceAccessNC
 * Signature: (Ljava/lang/Runnable;)I
 */
JNIEXPORT jint JNICALL Java_javax_comm_CommPortIdentifier_monitorInterVMDeviceAccessNC
  (JNIEnv *, jobject, jobject);
#ifdef __cplusplus
}
#endif
#endif
