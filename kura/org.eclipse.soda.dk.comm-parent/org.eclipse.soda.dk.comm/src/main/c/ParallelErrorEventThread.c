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
#include "org_eclipse_soda_dk_comm_ParallelErrorEventThread.h"
#else
#include <org_eclipse_soda_dk_comm_ParallelErrorEventThread.h>
#endif
/*
 * Class:     org_eclipse_soda_dk_comm_ParallelErrorEventThread
 * Method:    monitorParallelErrorNC
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_org_eclipse_soda_dk_comm_ParallelErrorEventThread_monitorParallelErrorNC
(JNIEnv *jenv, jobject jobj, jint jfd) {
#ifdef WIN32
	return;
//    return w32ParallelErrorEventThread_monitorParallelErrorNC(jenv, jobj, jfd);
#else
    return;
//    return cygParallelErrorEventThread_monitorParallelErrorNC(jenv, jobj, jfd);
#endif
} /* Java_org_eclipse_soda_dk_comm_ParallelErrorEventThread_monitorParallelErrorNC */
