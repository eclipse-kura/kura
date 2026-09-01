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
#include "org_eclipse_soda_dk_comm_NSParallelPort.h"
#include "w32ParallePort.h"
#else
#include <org_eclipse_soda_dk_comm_NSParallelPort.h>
#endif
/*
 * Class:     org_eclipse_soda_dk_comm_NSParallelPort
 * Method:    closeDeviceNC
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL  Java_org_eclipse_soda_dk_comm_NSParallelPort_closeDeviceNC
  (JNIEnv *jenv, jobject jobj, jint fd, jint semId)
{
#ifdef WIN32
	return w32ParallelPort_closeDeviceNC(jenv, jobj, fd, semId);
#else
//    return cyg32ParallelPort_closeDeviceNC(jenv, jobj, fd, semId);
    return cygParallelPort_closeDeviceNC(jenv, jobj, fd, semId);
#endif
}	/* Java_org_eclipse_soda_dk_comm_NSParallelPort_closeDeviceNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSParallelPort
 * Method:    openDeviceNC
 * Signature: (Ljava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_soda_dk_comm_NSParallelPort_openDeviceNC
  (JNIEnv *jenv, jobject jobj, jstring name, jint semId)
{
#ifdef WIN32
    return w32ParallelPort_openDeviceNC(jenv, jobj, name, semId);
#else
    return cygParallelPort_openDeviceNC(jenv, jobj, name, semId);
#endif
}   /* Java_org_eclipse_soda_dk_comm_NSParallelPort_openDeviceNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSParallelPort
 * Method:    isPaperOutNC
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_soda_dk_comm_NSParallelPort_isPaperOutNC
  (JNIEnv *jenv, jobject jobj, jint jfd)
{
#ifdef WIN32
	return 1;
//    return w32ParallelPort_isPaperOutNC(jenv, jobj, jfd);
#else
    return cygParallelPort_isPaperOutNC(jenv, jobj, jfd);
#endif
}	/* Java_org_eclipse_soda_dk_comm_NSParallelPort_isPaperOutNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSParallelPort
 * Method:    isPrinterBusyNC
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_soda_dk_comm_NSParallelPort_isPrinterBusyNC
  (JNIEnv *jenv, jobject jobj, jint jfd)
{
#ifdef WIN32
	return 1;
//    return w32ParallelPort_isPrinterBusyNC(jenv, jobj, jfd);
#else
    return cygParallelPort_isPrinterBusyNC(jenv, jobj, jfd);
#endif
}	/* Java_org_eclipse_soda_dk_comm_NSParallelPort_isPrinterBusyNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSParallelPort
 * Method:    isPrinterSelectedNC
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_soda_dk_comm_NSParallelPort_isPrinterSelectedNC
  (JNIEnv *jenv, jobject jobj, jint jfd)
{
#ifdef WIN32
	return 1;
//    return w32ParallelPort_isPrinterSelectedNC(jenv, jobj, jfd);
#else
    return cygParallelPort_isPrinterSelectedNC(jenv, jobj, jfd);
#endif
}	/* Java_org_eclipse_soda_dk_comm_NSParallelPort_isPrinterSelectedNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSParallelPort
 * Method:    isPrinterTimedOutNC
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_soda_dk_comm_NSParallelPort_isPrinterTimedOutNC
  (JNIEnv *jenv, jobject jobj, jint jfd)
{
#ifdef WIN32
	return 1;
//    return w32ParallelPort_isPrinterTimedOutNC(jenv, jobj, jfd);
#else
    return cygParallelPort_isPrinterTimedOutNC(jenv, jobj, jfd);
#endif
}	/* Java_org_eclipse_soda_dk_comm_NSParallelPort_isPrinterTimedOutNC */
/*
 * Class:     org_eclipse_soda_dk_comm_NSParallelPort
 * Method:    isPrinterErrorNC
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_org_eclipse_soda_dk_comm_NSParallelPort_isPrinterErrorNC
  (JNIEnv *jenv, jobject jobj, jint jfd)
{
#ifdef WIN32
	return 1;
//    return w32ParallelPort_isPrinterErrorNC(jenv, jobj, jfd);
#else
    return cygParallelPort_isPrinterErrorNC(jenv, jobj, jfd);
#endif
}	/* Java_org_eclipse_soda_dk_comm_NSParallelPort_isPrinterErrorNC */
