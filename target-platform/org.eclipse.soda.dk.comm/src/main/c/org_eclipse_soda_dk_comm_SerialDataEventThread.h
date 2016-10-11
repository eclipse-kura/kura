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
/* Header for class org_eclipse_soda_dk_comm_SerialDataEventThread */
#ifndef _Included_org_eclipse_soda_dk_comm_SerialDataEventThread
#define _Included_org_eclipse_soda_dk_comm_SerialDataEventThread
#ifdef __cplusplus
extern "C" {
#endif
#undef org_eclipse_soda_dk_comm_SerialDataEventThread_MAX_PRIORITY
#define org_eclipse_soda_dk_comm_SerialDataEventThread_MAX_PRIORITY 10L
#undef org_eclipse_soda_dk_comm_SerialDataEventThread_MIN_PRIORITY
#define org_eclipse_soda_dk_comm_SerialDataEventThread_MIN_PRIORITY 1L
#undef org_eclipse_soda_dk_comm_SerialDataEventThread_NORM_PRIORITY
#define org_eclipse_soda_dk_comm_SerialDataEventThread_NORM_PRIORITY 5L
#undef org_eclipse_soda_dk_comm_SerialDataEventThread_NANOS_MAX
#define org_eclipse_soda_dk_comm_SerialDataEventThread_NANOS_MAX 999999L
#undef org_eclipse_soda_dk_comm_SerialDataEventThread_INITIAL_LOCAL_STORAGE_CAPACITY
#define org_eclipse_soda_dk_comm_SerialDataEventThread_INITIAL_LOCAL_STORAGE_CAPACITY 5L
#undef org_eclipse_soda_dk_comm_SerialDataEventThread_NO_REF
#define org_eclipse_soda_dk_comm_SerialDataEventThread_NO_REF 0L
/*
 * Class:     org_eclipse_soda_dk_comm_SerialDataEventThread
 * Method:    monitorSerialDataNC
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_org_eclipse_soda_dk_comm_SerialDataEventThread_monitorSerialDataNC
  (JNIEnv *, jobject, jint);
#ifdef __cplusplus
}
#endif
#endif
