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
#include "javax_comm_CommPortIdentifier.h"
extern int w32CommPortIdentifier_monitorInterJVMDeviceAccessNC( JNIEnv *, jobject, jobject );
#else
#if _WIN32_WCE>=400
#include "javax_comm_CommPortIdentifier.h"
extern int w32CommPortIdentifier_monitorInterJVMDeviceAccessNC( JNIEnv *, jobject, jobject );
#else //linux
#include <javax_comm_CommPortIdentifier.h>
#endif //_WIN32_WCE>=400
#endif //WIN32
/*
 * Class:     javax_comm_CommPortIdentifier
 * Method:    monitorInterJVMDeviceAccessNC
 * Signature: (Ljava/lang/Thread;)I
 *
 * Currenty not Supported on Posix Devices
 */
JNIEXPORT jint JNICALL Java_javax_comm_CommPortIdentifier_monitorInterJVMDeviceAccessNC
				(JNIEnv *jenv, jobject jobj, jobject jtho) {
#ifdef WIN32
    return w32CommPortIdentifier_monitorInterJVMDeviceAccessNC( jenv, jobj, jtho );
#else
#if _WIN32_WCE>=400
    return w32CommPortIdentifier_monitorInterJVMDeviceAccessNC( jenv, jobj, jtho );
#else
    return cygCommPortIdentifier_monitorInterJVMDeviceAccessNC( jenv, jobj, jtho );
#endif
#endif
} /* Java_javax_comm_CommPortIdentifier_monitorInterJVMDeviceAccessNC */
