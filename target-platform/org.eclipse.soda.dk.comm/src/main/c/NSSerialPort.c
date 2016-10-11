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
#include "org_eclipse_soda_dk_comm_NSSerialPort.h"
#include "w32SerialPort.h"
#else
#if _WIN32_WCE>=400
#include "org_eclipse_soda_dk_comm_NSSerialPort.h"
#include "w32SerialPort.h"
#else
#include <org_eclipse_soda_dk_comm_NSSerialPort.h>
#endif
#endif
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    closeDeviceNC
 * Signature: (Ljava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_closeDeviceNC
  (JNIEnv *jenv, jobject jobj, jint fd, jint semId)
{
#ifdef WIN32
    return w32SerialPort_closeDeviceNC(jenv, jobj, fd, semId);
#else
#if _WIN32_WCE>=400
	return w32SerialPort_closeDeviceNC(jenv, jobj, fd, semId);
#else
    return cygSerialPort_closeDeviceNC(jenv, jobj, fd, semId);
#endif
#endif
}	/* Java_org_eclipse_soda_dk_comm_NSSerialPort_closeDeviceNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    openDeviceNC
 * Signature: (Ljava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_openDeviceNC
  (JNIEnv *jenv, jobject jobj, jstring name, jint semId)
{
#ifdef WIN32
    return w32SerialPort_openDeviceNC((JNIEnv *)jenv, jobj, name, semId);
#else
#if _WIN32_WCE>=400
    return w32SerialPort_openDeviceNC((JNIEnv *)jenv, jobj, name, semId);
#else
    return cygSerialPort_openDeviceNC(jenv, jobj, name, semId);
#endif
#endif
}   /* Java_org_eclipse_soda_dk_comm_NSSerialPort_openDeviceNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    setFlowControlModeNC
 * Signature: (Ljava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_setFlowControlModeNC
  (JNIEnv *jenv, jobject jobj, jint fd, jint fc )
{
#ifdef WIN32
    return w32SerialPort_setFlowControlModeNC(jenv, jobj, fd, fc );
#else
#if _WIN32_WCE>=400
    return w32SerialPort_setFlowControlModeNC(jenv, jobj, fd, fc );
#else
    return cygSerialPort_setFlowControlModeNC(jenv, jobj, fd, fc );
#endif
#endif
}   /* Java_org_eclipse_soda_dk_comm_NSSerialPort_setFlowControlModeNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    getFlowControlModeNC
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_getFlowControlModeNC
  (JNIEnv *jenv, jobject jobj, jint fd )
{
#ifdef WIN32
    return w32SerialPort_getFlowControlModeNC(jenv, jobj, fd );
#else
#if _WIN32_WCE>=400
    return w32SerialPort_getFlowControlModeNC(jenv, jobj, fd );
#else
    return cygSerialPort_getFlowControlModeNC(jenv, jobj, fd );
#endif
#endif
}  /* Java_org_eclipse_soda_dk_comm_NSSerialPort_getFlowControlModeNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    getBaudRateNC
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_getBaudRateNC
  (JNIEnv *jenv, jobject jobj, jint fd)
{
#ifdef WIN32
    return w32SerialPort_getBaudRateNC(jenv, jobj, fd);
#else
#if _WI32_WCE>=400
    return w32SerialPort_getBaudRateNC(jenv, jobj, fd);
#else
    return cygSerialPort_getBaudRateNC(jenv, jobj, fd);
#endif
#endif
}  /* Java_org_eclipse_soda_dk_comm_NSSerialPort_getBaudRateNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    getDataBitsNC
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_getDataBitsNC
  (JNIEnv *jenv, jobject jobj, jint fd)
{
#ifdef WIN32
    return w32SerialPort_getDataBitsNC(jenv, jobj, fd);
#else
#if _WIN32_WCE>=400
    return w32SerialPort_getDataBitsNC(jenv, jobj, fd);
#else
    return cygSerialPort_getDataBitsNC(jenv, jobj, fd);
#endif
#endif
}  /* Java_org_eclipse_soda_dk_comm_NSSerialPort_getDataBitsNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    getStopBitsNC
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_getStopBitsNC
  (JNIEnv *jenv, jobject jobj, jint fd)
{
#ifdef WIN32
    return w32SerialPort_getStopBitsNC( jenv, jobj, fd );
#else
#if _WIN32_WCE>=400
    return w32SerialPort_getStopBitsNC( jenv, jobj, fd );
#else
    return cygSerialPort_getStopBitsNC( jenv, jobj, fd );
#endif
#endif
}  /* Java_org_eclipse_soda_dk_comm_NSSerialPort_getStopBitsNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    getParityNC
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_getParityNC
  (JNIEnv *jenv, jobject jobj, jint fd)
{
#ifdef WIN32
    return w32SerialPort_getParityNC( jenv, jobj, fd );
#else
#if _WIN32_WCE>=400
    return w32SerialPort_getParityNC( jenv, jobj, fd );
#else
    return cygSerialPort_getParityNC( jenv, jobj, fd );
#endif
#endif
}  /* Java_org_eclipse_soda_dk_comm_NSSerialPort_getParityNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    setDTRNC
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_setDTRNC
  (JNIEnv *jenv, jobject jobj, jboolean bool)
{
#ifdef WIN32
    w32SerialPort_setDTRNC( jenv, jobj, bool );
#else
#if _WIN32_WCE>=400
    w32SerialPort_setDTRNC( jenv, jobj, bool );
#else
    cygSerialPort_setDTRNC( jenv, jobj, bool );
#endif
#endif
}  /* Java_org_eclipse_soda_dk_comm_NSSerialPort_setDTRNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    isDTRNC
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_isDTRNC
  (JNIEnv *jenv, jobject jobj)
{
#ifdef WIN32
    return w32SerialPort_isDTRNC( jenv, jobj );
#else
#if _WIN32_WCE
    return w32SerialPort_isDTRNC( jenv, jobj );
#else
    return cygSerialPort_isDTRNC( jenv, jobj );
#endif
#endif
}  /* Java_org_eclipse_soda_dk_comm_NSSerialPort_isDTRNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    setRTSNC
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_setRTSNC
  (JNIEnv *jenv, jobject jobj, jboolean bool)
{
#ifdef WIN32
    w32SerialPort_setRTSNC( jenv, jobj, bool );
#else
#if _WIN32_WCE>=400
    w32SerialPort_setRTSNC( jenv, jobj, bool );
#else
    cygSerialPort_setRTSNC( jenv, jobj, bool );
#endif
#endif
}  /* Java_org_eclipse_soda_dk_comm_NSSerialPort_setRTSNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    isRTSNC
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_isRTSNC
  (JNIEnv *jenv, jobject jobj)
{
#ifdef WIN32
    return w32SerialPort_isRTSNC( jenv, jobj );
#else
#if _WIN32_WCE>=400
    return w32SerialPort_isRTSNC( jenv, jobj );
#else
    return cygSerialPort_isRTSNC( jenv, jobj );
#endif
#endif
}  /* Java_org_eclipse_soda_dk_comm_NSSerialPort_isRTSNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    isCTSNC
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_isCTSNC
  (JNIEnv *jenv, jobject jobj)
{
#ifdef WIN32
    return w32SerialPort_isCTSNC( jenv, jobj );
#else
#if _WIN32_WCE>=400
    return w32SerialPort_isCTSNC( jenv, jobj );
#else
    return cygSerialPort_isCTSNC( jenv, jobj );
#endif
#endif
}  /* Java_org_eclipse_soda_dk_comm_NSSerialPort_isCTSNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    isDSRNC
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_isDSRNC
  (JNIEnv *jenv, jobject jobj)
{
#ifdef WIN32
    return w32SerialPort_isDSRNC( jenv, jobj );
#else
#ifdef _WIN32_WCE>=400
    return w32SerialPort_isDSRNC( jenv, jobj );
#else
    return cygSerialPort_isDSRNC( jenv, jobj );
#endif
#endif
}  /* Java_org_eclipse_soda_dk_comm_NSSerialPort_isDSRNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    isRINC
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_isRINC
  (JNIEnv *jenv, jobject jobj)
{
#ifdef WIN32
    return w32SerialPort_isRINC( jenv, jobj );
#else
#if _WIN32_WCE>=400
    return w32SerialPort_isRINC( jenv, jobj );
#else
    return cygSerialPort_isRINC( jenv, jobj );
#endif
#endif
}  /* Java_org_eclipse_soda_dk_comm_NSSerialPort_isRINC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    isCDNC
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_isCDNC
  (JNIEnv *jenv, jobject jobj)
{
#ifdef WIN32
    return w32SerialPort_isCDNC( jenv, jobj );
#else
#if _WIN32_WCE>=400
    return w32SerialPort_isCDNC( jenv, jobj );
#else
    return cygSerialPort_isCDNC( jenv, jobj );
#endif
#endif
}  /* Java_org_eclipse_soda_dk_comm_NSSerialPort_isCDNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    sendBreakNC
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_sendBreakNC
  (JNIEnv *jenv, jobject jobj, jint jfd, jint jmillis) {
#ifdef WIN32
	return 1;
//    return w32SerialPort_sendBreakNC( jenv, jobj, jfd, jmillis );
#else
#if _WIN32_WCE>=400
	return 1;
#else
    return cygSerialPort_sendBreakNC( jenv, jobj, jfd, jmillis );
#endif
#endif
} /* Java_org_eclipse_soda_dk_comm_NSSerialPort_sendBreakNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSSerialPort
 * Method:    setSerialPortParamsNC
 * Signature: (IIIII)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_soda_dk_comm_NSSerialPort_setSerialPortParamsNC
  (JNIEnv *jenv, jobject jobj, jint jfd, jint jbd, jint jdb, jint jsb, jint jpar) {
#ifdef WIN32
    return w32SerialPort_setSerialPortParamsNC( jenv, jobj, jfd, jbd, jdb, jsb, jpar );
#else
#if _WIN32_WCE>=400
    return w32SerialPort_setSerialPortParamsNC( jenv, jobj, jfd, jbd, jdb, jsb, jpar );
#else
    return cygSerialPort_setSerialPortParamsNC( jenv, jobj, jfd, jbd, jdb, jsb, jpar );
#endif
#endif
} /* Java_org_eclipse_soda_dk_comm_NSSerialPort_setSerialPortParamsNC */
