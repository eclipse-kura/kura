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
/* Header for class org_eclipse_soda_dk_comm_NSSerialPort */
#ifndef _Included_org_eclipse_soda_dk_comm_NSSerialPort
#define _Included_org_eclipse_soda_dk_comm_NSSerialPort
#ifdef __cplusplus
extern "C" {
#endif
#undef org_eclipse_soda_dk_comm_NSSerialPort_DATABITS_5
#define org_eclipse_soda_dk_comm_NSSerialPort_DATABITS_5 5L
#undef org_eclipse_soda_dk_comm_NSSerialPort_DATABITS_6
#define org_eclipse_soda_dk_comm_NSSerialPort_DATABITS_6 6L
#undef org_eclipse_soda_dk_comm_NSSerialPort_DATABITS_7
#define org_eclipse_soda_dk_comm_NSSerialPort_DATABITS_7 7L
#undef org_eclipse_soda_dk_comm_NSSerialPort_DATABITS_8
#define org_eclipse_soda_dk_comm_NSSerialPort_DATABITS_8 8L
#undef org_eclipse_soda_dk_comm_NSSerialPort_STOPBITS_1
#define org_eclipse_soda_dk_comm_NSSerialPort_STOPBITS_1 1L
#undef org_eclipse_soda_dk_comm_NSSerialPort_STOPBITS_2
#define org_eclipse_soda_dk_comm_NSSerialPort_STOPBITS_2 2L
#undef org_eclipse_soda_dk_comm_NSSerialPort_STOPBITS_1_5
#define org_eclipse_soda_dk_comm_NSSerialPort_STOPBITS_1_5 3L
#undef org_eclipse_soda_dk_comm_NSSerialPort_PARITY_NONE
#define org_eclipse_soda_dk_comm_NSSerialPort_PARITY_NONE 0L
#undef org_eclipse_soda_dk_comm_NSSerialPort_PARITY_ODD
#define org_eclipse_soda_dk_comm_NSSerialPort_PARITY_ODD 1L
#undef org_eclipse_soda_dk_comm_NSSerialPort_PARITY_EVEN
#define org_eclipse_soda_dk_comm_NSSerialPort_PARITY_EVEN 2L
#undef org_eclipse_soda_dk_comm_NSSerialPort_PARITY_MARK
#define org_eclipse_soda_dk_comm_NSSerialPort_PARITY_MARK 3L
#undef org_eclipse_soda_dk_comm_NSSerialPort_PARITY_SPACE
#define org_eclipse_soda_dk_comm_NSSerialPort_PARITY_SPACE 4L
#undef org_eclipse_soda_dk_comm_NSSerialPort_FLOWCONTROL_NONE
#define org_eclipse_soda_dk_comm_NSSerialPort_FLOWCONTROL_NONE 0L
#undef org_eclipse_soda_dk_comm_NSSerialPort_FLOWCONTROL_RTSCTS_IN
#define org_eclipse_soda_dk_comm_NSSerialPort_FLOWCONTROL_RTSCTS_IN 1L
#undef org_eclipse_soda_dk_comm_NSSerialPort_FLOWCONTROL_RTSCTS_OUT
#define org_eclipse_soda_dk_comm_NSSerialPort_FLOWCONTROL_RTSCTS_OUT 2L
#undef org_eclipse_soda_dk_comm_NSSerialPort_FLOWCONTROL_XONXOFF_IN
#define org_eclipse_soda_dk_comm_NSSerialPort_FLOWCONTROL_XONXOFF_IN 4L
#undef org_eclipse_soda_dk_comm_NSSerialPort_FLOWCONTROL_XONXOFF_OUT
#define org_eclipse_soda_dk_comm_NSSerialPort_FLOWCONTROL_XONXOFF_OUT 8L
#undef org_eclipse_soda_dk_comm_NSSerialPort_DATABITS_5
#define org_eclipse_soda_dk_comm_NSSerialPort_DATABITS_5 5L
#undef org_eclipse_soda_dk_comm_NSSerialPort_DATABITS_6
#define org_eclipse_soda_dk_comm_NSSerialPort_DATABITS_6 6L
#undef org_eclipse_soda_dk_comm_NSSerialPort_DATABITS_7
#define org_eclipse_soda_dk_comm_NSSerialPort_DATABITS_7 7L
#undef org_eclipse_soda_dk_comm_NSSerialPort_DATABITS_8
#define org_eclipse_soda_dk_comm_NSSerialPort_DATABITS_8 8L
#undef org_eclipse_soda_dk_comm_NSSerialPort_STOPBITS_1
#define org_eclipse_soda_dk_comm_NSSerialPort_STOPBITS_1 1L
#undef org_eclipse_soda_dk_comm_NSSerialPort_STOPBITS_2
#define org_eclipse_soda_dk_comm_NSSerialPort_STOPBITS_2 2L
#undef org_eclipse_soda_dk_comm_NSSerialPort_STOPBITS_1_5
#define org_eclipse_soda_dk_comm_NSSerialPort_STOPBITS_1_5 3L
#undef org_eclipse_soda_dk_comm_NSSerialPort_PARITY_NONE
#define org_eclipse_soda_dk_comm_NSSerialPort_PARITY_NONE 0L
#undef org_eclipse_soda_dk_comm_NSSerialPort_PARITY_ODD
#define org_eclipse_soda_dk_comm_NSSerialPort_PARITY_ODD 1L
#undef org_eclipse_soda_dk_comm_NSSerialPort_PARITY_EVEN
#define org_eclipse_soda_dk_comm_NSSerialPort_PARITY_EVEN 2L
#undef org_eclipse_soda_dk_comm_NSSerialPort_PARITY_MARK
#define org_eclipse_soda_dk_comm_NSSerialPort_PARITY_MARK 3L
#undef org_eclipse_soda_dk_comm_NSSerialPort_PARITY_SPACE
#define org_eclipse_soda_dk_comm_NSSerialPort_PARITY_SPACE 4L
#undef org_eclipse_soda_dk_comm_NSSerialPort_FLOWCONTROL_NONE
#define org_eclipse_soda_dk_comm_NSSerialPort_FLOWCONTROL_NONE 0L
#undef org_eclipse_soda_dk_comm_NSSerialPort_FLOWCONTROL_RTSCTS_IN
#define org_eclipse_soda_dk_comm_NSSerialPort_FLOWCONTROL_RTSCTS_IN 1L
#undef org_eclipse_soda_dk_comm_NSSerialPort_FLOWCONTROL_RTSCTS_OUT
#define org_eclipse_soda_dk_comm_NSSerialPort_FLOWCONTROL_RTSCTS_OUT 2L
#undef org_eclipse_soda_dk_comm_NSSerialPort_FLOWCONTROL_XONXOFF_IN
#define org_eclipse_soda_dk_comm_NSSerialPort_FLOWCONTROL_XONXOFF_IN 4L
#undef org_eclipse_soda_dk_comm_NSSerialPort_FLOWCONTROL_XONXOFF_OUT
#define org_eclipse_soda_dk_comm_NSSerialPort_FLOWCONTROL_XONXOFF_OUT 8L
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    openDeviceNC
 * Signature: (Ljava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_openDeviceNC
  (JNIEnv *, jobject, jstring, jint);
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    closeDeviceNC
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_closeDeviceNC
  (JNIEnv *, jobject, jint, jint);
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    setFlowControlModeNC
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_setFlowControlModeNC
  (JNIEnv *, jobject, jint, jint);
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    getFlowControlModeNC
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_getFlowControlModeNC
  (JNIEnv *, jobject, jint);
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    sendBreakNC
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_sendBreakNC
  (JNIEnv *, jobject, jint, jint);
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    getBaudRateNC
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_getBaudRateNC
  (JNIEnv *, jobject, jint);
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    getDataBitsNC
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_getDataBitsNC
  (JNIEnv *, jobject, jint);
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    getStopBitsNC
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_getStopBitsNC
  (JNIEnv *, jobject, jint);
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    getParityNC
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_getParityNC
  (JNIEnv *, jobject, jint);
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    setDTRNC
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_setDTRNC
  (JNIEnv *, jobject, jboolean);
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    setRTSNC
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_setRTSNC
  (JNIEnv *, jobject, jboolean);
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    setSerialPortParamsNC
 * Signature: (IIIII)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_setSerialPortParamsNC
  (JNIEnv *, jobject, jint, jint, jint, jint, jint);
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    isDTRNC
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_isDTRNC
  (JNIEnv *, jobject);
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    isRTSNC
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_isRTSNC
  (JNIEnv *, jobject);
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    isCTSNC
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_isCTSNC
  (JNIEnv *, jobject);
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    isDSRNC
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_isDSRNC
  (JNIEnv *, jobject);
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    isRINC
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_isRINC
  (JNIEnv *, jobject);
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    isCDNC
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_isCDNC
  (JNIEnv *, jobject);
#ifdef __cplusplus
}
#endif
#endif
