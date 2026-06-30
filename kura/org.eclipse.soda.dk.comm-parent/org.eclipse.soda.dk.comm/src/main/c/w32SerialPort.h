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
#include <windows.h>
int  w32SerialPort_closeDeviceNC( JNIEnv *, jobject, jint, jint );
int  w32SerialPort_openDeviceNC( JNIEnv *, jobject, jstring, jint );
int  w32SerialPort_getBaudRateNC( JNIEnv *, jobject, jint );
int  w32SerialPort_getDataBitsNC( JNIEnv *, jobject, jint );
int  w32SerialPort_getStopBitsNC( JNIEnv *, jobject, jint );
int  w32SerialPort_getParityNC( JNIEnv *, jobject, jint );
void w32SerialPort_setDTRNC( JNIEnv *, jobject, jboolean );
void w32SerialPort_setRTSNC( JNIEnv *, jobject, jboolean );
BOOL w32SerialPort_isDTRNC( JNIEnv *, jobject );
BOOL w32SerialPort_isRTSNC( JNIEnv *, jobject );
BOOL w32SerialPort_isCTSNC( JNIEnv *, jobject );
BOOL w32SerialPort_isDSRNC( JNIEnv *, jobject );
BOOL w32SerialPort_isRINC(  JNIEnv *, jobject );
BOOL w32SerialPort_isCDNC(  JNIEnv *, jobject );
int  w32SerialPort_setFlowControlModeNC( JNIEnv *, jobject, jint, jint );
int  w32SerialPort_getFlowControlModeNC(JNIEnv *, jobject, jint );
int  w32SerialPort_setSerialPortParamsNC( JNIEnv *, jobject, jint, jint, jint, jint, jint );
