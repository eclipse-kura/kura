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
/* Header for class org_eclipse_soda_dk_comm_NSParallelPort */
#ifndef _Included_org_eclipse_soda_dk_comm_NSParallelPort
#define _Included_org_eclipse_soda_dk_comm_NSParallelPort
#ifdef __cplusplus
extern "C" {
#endif
#undef org_eclipse_soda_dk_comm_NSParallelPort_LPT_MODE_ANY
#define org_eclipse_soda_dk_comm_NSParallelPort_LPT_MODE_ANY 0L
#undef org_eclipse_soda_dk_comm_NSParallelPort_LPT_MODE_SPP
#define org_eclipse_soda_dk_comm_NSParallelPort_LPT_MODE_SPP 1L
#undef org_eclipse_soda_dk_comm_NSParallelPort_LPT_MODE_PS2
#define org_eclipse_soda_dk_comm_NSParallelPort_LPT_MODE_PS2 2L
#undef org_eclipse_soda_dk_comm_NSParallelPort_LPT_MODE_EPP
#define org_eclipse_soda_dk_comm_NSParallelPort_LPT_MODE_EPP 3L
#undef org_eclipse_soda_dk_comm_NSParallelPort_LPT_MODE_ECP
#define org_eclipse_soda_dk_comm_NSParallelPort_LPT_MODE_ECP 4L
#undef org_eclipse_soda_dk_comm_NSParallelPort_LPT_MODE_NIBBLE
#define org_eclipse_soda_dk_comm_NSParallelPort_LPT_MODE_NIBBLE 5L
#undef org_eclipse_soda_dk_comm_NSParallelPort_LPT_MODE_ANY
#define org_eclipse_soda_dk_comm_NSParallelPort_LPT_MODE_ANY 0L
#undef org_eclipse_soda_dk_comm_NSParallelPort_LPT_MODE_SPP
#define org_eclipse_soda_dk_comm_NSParallelPort_LPT_MODE_SPP 1L
#undef org_eclipse_soda_dk_comm_NSParallelPort_LPT_MODE_PS2
#define org_eclipse_soda_dk_comm_NSParallelPort_LPT_MODE_PS2 2L
#undef org_eclipse_soda_dk_comm_NSParallelPort_LPT_MODE_EPP
#define org_eclipse_soda_dk_comm_NSParallelPort_LPT_MODE_EPP 3L
#undef org_eclipse_soda_dk_comm_NSParallelPort_LPT_MODE_ECP
#define org_eclipse_soda_dk_comm_NSParallelPort_LPT_MODE_ECP 4L
#undef org_eclipse_soda_dk_comm_NSParallelPort_LPT_MODE_NIBBLE
#define org_eclipse_soda_dk_comm_NSParallelPort_LPT_MODE_NIBBLE 5L
/*
 * Class:     org_eclipse_soda_dk_comm_NSParallelPort
 * Method:    openDeviceNC
 * Signature: (Ljava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_soda_dk_comm_NSParallelPort_openDeviceNC
  (JNIEnv *, jobject, jstring, jint);
/*
 * Class:     org_eclipse_soda_dk_comm_NSParallelPort
 * Method:    closeDeviceNC
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_soda_dk_comm_NSParallelPort_closeDeviceNC
  (JNIEnv *, jobject, jint, jint);
/*
 * Class:     org_eclipse_soda_dk_comm_NSParallelPort
 * Method:    isPaperOutNC
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_soda_dk_comm_NSParallelPort_isPaperOutNC
  (JNIEnv *, jobject, jint);
/*
 * Class:     org_eclipse_soda_dk_comm_NSParallelPort
 * Method:    isPrinterBusyNC
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_soda_dk_comm_NSParallelPort_isPrinterBusyNC
  (JNIEnv *, jobject, jint);
/*
 * Class:     org_eclipse_soda_dk_comm_NSParallelPort
 * Method:    isPrinterSelectedNC
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_soda_dk_comm_NSParallelPort_isPrinterSelectedNC
  (JNIEnv *, jobject, jint);
/*
 * Class:     org_eclipse_soda_dk_comm_NSParallelPort
 * Method:    isPrinterTimedOutNC
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_soda_dk_comm_NSParallelPort_isPrinterTimedOutNC
  (JNIEnv *, jobject, jint);
/*
 * Class:     org_eclipse_soda_dk_comm_NSParallelPort
 * Method:    isPrinterErrorNC
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_soda_dk_comm_NSParallelPort_isPrinterErrorNC
  (JNIEnv *, jobject, jint);
#ifdef __cplusplus
}
#endif
#endif
