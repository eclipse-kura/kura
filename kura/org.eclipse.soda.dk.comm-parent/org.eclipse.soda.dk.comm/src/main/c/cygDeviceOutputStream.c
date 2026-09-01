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
#include <stdio.h>
#include <org_eclipse_soda_dk_comm_NSDeviceOutputStream.h>
#define assert(s) if (!s) {printf("\n\n%d asserted!\n\n", __LINE__); return(-1);}
/*
 * Class:     org_eclipse_soda_dk_comm_NSDeviceOutputStream
 * Method:    writeDeviceNC
 * Signature: ([BII)I
 */
int cygDeviceOutputStream_writeDeviceNC
  (JNIEnv *jenv, jobject jobj, jbyteArray jbuf, jint off, jint len) {
  jclass	jc;
  jfieldID	jf;
  jint 		fd = -1;
  jbyte		*cbuf;
  jboolean	isCopy;
  int		wc = 0;
  jbyte		*cb;
  int		rc;
  if (!len)
	return wc;
  // Get the file descriptor.
  jc = (*jenv)->GetObjectClass(jenv, jobj);
  assert(jc);
  jf = (*jenv)->GetFieldID(jenv, jc, "fd", "I");
  assert(jf);
  fd = (*jenv)->GetIntField(jenv, jobj, jf);
  if (fd == -1)
	return -1;
  // Convert the java byte array buffer into c byte buffer.
  cbuf =  (*jenv)->GetByteArrayElements(jenv, jbuf, &isCopy);
  // Write the data out to the device.
  for ( cb = cbuf+off; len; len -= rc, wc += rc, cb += rc ) {
	if ((rc = write(fd, cb, len)) < 0)
		break;
  }
  // Should we throw some exception in the event of a write error ????
  // Free the c byte buffer.
  (*jenv)->ReleaseByteArrayElements(jenv, jbuf, cbuf, JNI_ABORT);
  return wc;
}	/* cygDeviceOutputStream_writeDeviceNC */
