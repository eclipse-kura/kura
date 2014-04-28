/*
 * Java Unix Sockets Library
 *
 * Copyright (c) Matthew Johnson 2005
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * 
 * To Contact the author, please email src@matthew.ath.cx
 *
 */


/* _GNU_SOURCE is required to use struct ucred in glibc 2.8 */
#define _GNU_SOURCE

#include "unix-java.h"
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/time.h>
#include <sys/socket.h>
#include <sys/select.h>
#include <errno.h>
#include <string.h>
#include <sys/un.h>

#ifndef IOV_MAX
#define IOV_MAX 1024
#endif


#ifdef __cplusplus
extern "C" {
#endif

void throw(JNIEnv* env, int err, const char* msg)
{
   jstring jmsg = (*env)->NewStringUTF(env, msg);
   jclass exc = (*env)->FindClass(env, "cx/ath/matthew/unix/UnixIOException");
   jmethodID cons = (*env)->GetMethodID(env, exc, "<init>", "(ILjava/lang/String;)V");
   jobject exo = (*env)->NewObject(env, exc, cons, err, jmsg);
   (*env)->DeleteLocalRef(env, exc);
   (*env)->DeleteLocalRef(env, jmsg);
   (*env)->Throw(env, exo);
   (*env)->DeleteLocalRef(env, exo);
}

void handleerrno(JNIEnv *env)
{
   if (0 == errno) return;
   int err = errno;
   if (EAGAIN == err) return; // we read 0 bytes due to a timeout
   const char* msg = strerror(err);
   throw(env, err, msg);
}
   
/*
 * Class:     cx_ath_matthew_unix_UnixServerSocket
 * Method:    native_bind
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_cx_ath_matthew_unix_UnixServerSocket_native_1bind
  (JNIEnv *env, jobject o, jstring address, jboolean abstract)
{
   int sock = socket(PF_UNIX, SOCK_STREAM, 0);
   if (-1 == sock) { handleerrno(env); return -1; }
   const char* caddr = (*env)->GetStringUTFChars(env, address, 0);
   int slen = (*env)->GetStringUTFLength(env, address)+1;
   struct sockaddr_un *sad = malloc(sizeof(sa_family_t)+slen);
   if (abstract)  {
      char* shifted = sad->sun_path+1;
      strncpy(shifted, caddr, slen-1);
      sad->sun_path[0] = 0;
   } else
      strncpy(sad->sun_path, caddr, slen);
   (*env)->ReleaseStringUTFChars(env, address, caddr);
   sad->sun_family = AF_UNIX;
   int rv = bind(sock, (const  struct  sockaddr*) sad, sizeof(sa_family_t)+slen);
   free(sad);
   if (-1 == rv) { handleerrno(env); return -1; }
   rv = listen(sock, 10);
   if (-1 == rv) { handleerrno(env); return -1; }
   return sock;
}

/*
 * Class:     cx_ath_matthew_unix_UnixServerSocket
 * Method:    native_close
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_cx_ath_matthew_unix_UnixServerSocket_native_1close
  (JNIEnv * env, jobject o, jint sock)
{
   if (0 == sock) return;
   int rv = shutdown(sock, SHUT_RDWR);
   if (-1 == rv) { handleerrno(env); }
   else {
      rv = close(sock);
      if (-1 == rv) { handleerrno(env); }
   }
}

/*
 * Class:     cx_ath_matthew_unix_UnixServerSocket
 * Method:    native_accept
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_cx_ath_matthew_unix_UnixServerSocket_native_1accept
  (JNIEnv * env, jobject o, jint sock)
{
   int newsock = accept(sock, NULL, NULL);
   if (-1 == newsock) handleerrno(env);
   return newsock;
}

/*
 * Class:     cx_ath_matthew_unix_UnixSocket
 * Method:    native_set_pass_cred
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_cx_ath_matthew_unix_UnixSocket_native_1set_1pass_1cred
  (JNIEnv *env, jobject o, jint sock, jboolean enable)
{
#ifdef SO_PASSCRED
   int opt = enable;
   int rv = setsockopt(sock, SOL_SOCKET, SO_PASSCRED, &opt, sizeof(int));
   if (-1 == rv) { handleerrno(env);}
#endif
}

/*
 * Class:     cx_ath_matthew_unix_UnixSocket
 * Method:    native_connect
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_cx_ath_matthew_unix_UnixSocket_native_1connect
  (JNIEnv *env, jobject o, jstring address, jboolean abstract)
{
   int sock = socket(PF_UNIX, SOCK_STREAM, 0);
   if (-1 == sock) { handleerrno(env); return -1; }
   const char* caddr = (*env)->GetStringUTFChars(env, address, 0);
   int slen = (*env)->GetStringUTFLength(env, address)+1;
   struct sockaddr_un *sad = malloc(sizeof(sa_family_t)+slen);
   if (abstract)  {
      char* shifted = sad->sun_path+1;
      strncpy(shifted, caddr, slen-1);
      sad->sun_path[0] = 0;
   } else
      strncpy(sad->sun_path, caddr, slen);
   (*env)->ReleaseStringUTFChars(env, address, caddr);
   sad->sun_family = AF_UNIX;
   int rv = connect(sock, (const struct sockaddr*) sad, sizeof(sa_family_t)+slen);
   free(sad);
   if (-1 == rv) { handleerrno(env); return -1; }
   return sock;
}

/*
 * Class:     cx_ath_matthew_unix_UnixSocket
 * Method:    native_close
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_cx_ath_matthew_unix_UnixSocket_native_1close
  (JNIEnv *env, jobject o, jint sock)
{
   if (0 == sock) return;
   int rv = shutdown(sock, SHUT_RDWR);
   if (-1 == rv) { handleerrno(env); }
   else {
      rv = close(sock);
      if (-1 == rv) { handleerrno(env); }
   }
}

/*
 * Class:     cx_ath_matthew_unix_USInputStream
 * Method:    native_recv
 * Signature: ([BII)I
 */
JNIEXPORT jint JNICALL Java_cx_ath_matthew_unix_USInputStream_native_1recv
  (JNIEnv *env, jobject o, jint sock, jbyteArray buf, jint offs, jint len, jint flags, jint timeout)
{
   fd_set rfds;
   struct timeval tv;
   jbyte* cbuf = (*env)->GetByteArrayElements(env, buf, NULL);
   void* recvb = cbuf + offs;
   int rv;

   if (timeout > 0) {
      FD_ZERO(&rfds);
      FD_SET(sock, &rfds);
      tv.tv_sec = 0;
      tv.tv_usec = timeout;
      rv = select(sock+1, &rfds, NULL, NULL, &tv);
      rv = recv(sock, recvb, len, flags);
      if (-1 == rv) { handleerrno(env); rv = -1; }
      (*env)->ReleaseByteArrayElements(env, buf, cbuf, 0);
      return rv;
   } else  {
      rv = recv(sock, recvb, len, flags);
      (*env)->ReleaseByteArrayElements(env, buf, cbuf, 0);
      if (-1 == rv) { handleerrno(env); return -1; }
      return rv;
   }
}

/*
 * Class:     cx_ath_matthew_unix_USOutputStream
 * Method:    native_send
 * Signature: (I[BII)I
 */
JNIEXPORT jint JNICALL Java_cx_ath_matthew_unix_USOutputStream_native_1send__I_3BII
  (JNIEnv *env, jobject o, jint sock, jbyteArray buf, jint offs, jint len)
{
   jbyte* cbuf = (*env)->GetByteArrayElements(env, buf, NULL);
   void* sendb = cbuf + offs;
   int rv = send(sock, sendb, len, 0);
   (*env)->ReleaseByteArrayElements(env, buf, cbuf, 0);
   if (-1 == rv) { handleerrno(env); return -1; }
   return rv;
}

/*
 * Class:     cx_ath_matthew_unix_USOutputStream
 * Method:    native_send
 * Signature: (I[[B)I
 */
JNIEXPORT jint JNICALL Java_cx_ath_matthew_unix_USOutputStream_native_1send__I_3_3B
  (JNIEnv *env, jobject o, jint sock, jobjectArray bufs)
{
   size_t sblen = 1;
   socklen_t sblen_size = sizeof(sblen);
   getsockopt(sock, SOL_SOCKET, SO_SNDBUF, &sblen, &sblen_size);

   struct msghdr msg;
   struct iovec *iov;
   msg.msg_name = NULL;
   msg.msg_namelen = 0;
   msg.msg_control = NULL;
   msg.msg_controllen = 0;
   msg.msg_flags = 0;
   size_t els = (*env)->GetArrayLength(env, bufs);
   iov = (struct iovec*) malloc((els<IOV_MAX?els:IOV_MAX) * sizeof(struct iovec));
   msg.msg_iov = iov;
   jbyteArray *b = (jbyteArray*) malloc(els * sizeof(jbyteArray));
   int rv = 0;
   
   for (int i = 0, j = 0, s = 0; i <= els; i++, j++) {
      if (i == els) {
         msg.msg_iovlen = j;
         rv = sendmsg(sock, &msg, 0);
         for (int k = i-1, l = j-1; l >= 0; k--, l--)
            (*env)->ReleaseByteArrayElements(env, b[k], iov[l].iov_base, 0);
         if (-1 == rv) { handleerrno(env); return -1; }
         break;
      }
      b[i] = (*env)->GetObjectArrayElement(env, bufs, i);
      if (NULL == b[i]) {
         msg.msg_iovlen = j;
         rv = sendmsg(sock, &msg, 0);
         for (int k = i-1, l = j-1; l >= 0; k--, l--)
            (*env)->ReleaseByteArrayElements(env, b[k], iov[l].iov_base, 0);
         if (-1 == rv) { handleerrno(env); return -1; }
         break;
      }
      size_t l = (*env)->GetArrayLength(env, b[i]);
      if (s+l > sblen || j == IOV_MAX) {
         msg.msg_iovlen = j;
         rv = sendmsg(sock, &msg, 0);
         s = 0;
         for (int k = i-1, l = j-1; l >= 0; k--, l--)
            (*env)->ReleaseByteArrayElements(env, b[k], iov[l].iov_base, 0);
         j = 0;
         if (-1 == rv) { handleerrno(env); return -1; }
      }
      iov[j].iov_base = (*env)->GetByteArrayElements(env, b[i], NULL);
      iov[j].iov_len = l;
      s += l;
   }
   
   free(iov);
   free(b);
   return rv;
}

/*
 * Class:     cx_ath_matthew_unix_UnixSocket
 * Method:    native_getPID
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_cx_ath_matthew_unix_UnixSocket_native_1getPID
  (JNIEnv * env, jobject o, jint sock)
{
#ifdef SO_PEERCRED
   struct ucred cr;
   socklen_t cl=sizeof(cr);

   if (getsockopt(sock, SOL_SOCKET, SO_PEERCRED, &cr, &cl)==0) 
      return cr.pid;
   else
      return -1;
#else
   return -1;
#endif
}

/*
 * Class:     cx_ath_matthew_unix_UnixSocket
 * Method:    native_getUID
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_cx_ath_matthew_unix_UnixSocket_native_1getUID
  (JNIEnv * env, jobject o, jint sock)
{
#ifdef SO_PEERCRED
   struct ucred cr;
   socklen_t cl=sizeof(cr);

   if (getsockopt(sock, SOL_SOCKET, SO_PEERCRED, &cr, &cl)==0) 
      return cr.uid;
   else
      return -1;
#else
   return -1;
#endif
}

/*
 * Class:     cx_ath_matthew_unix_UnixSocket
 * Method:    native_getGID
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_cx_ath_matthew_unix_UnixSocket_native_1getGID
  (JNIEnv * env, jobject o, jint sock)
{
#ifdef SO_PEERCRED
   struct ucred cr;
   socklen_t cl=sizeof(cr);

   if (getsockopt(sock, SOL_SOCKET, SO_PEERCRED, &cr, &cl)==0) 
      return cr.gid;
   else
      return -1;
#else
   return -1;
#endif
}

/*
 * Class:     cx_ath_matthew_unix_UnixSocket
 * Method:    native_send_creds
 * Signature: (B)V
 */
JNIEXPORT void JNICALL Java_cx_ath_matthew_unix_UnixSocket_native_1send_1creds
  (JNIEnv * env, jobject o, jint sock, jbyte data)
{
   struct msghdr msg;
   struct iovec iov;
   msg.msg_name = NULL;
   msg.msg_namelen = 0;
   msg.msg_flags = 0;
   msg.msg_iov = &iov;
   msg.msg_iovlen = 1;
   msg.msg_control = NULL;
   msg.msg_controllen = 0;
   iov.iov_base = &data;
   iov.iov_len = 1;

#ifdef SCM_CREDENTIALS
   char buf[CMSG_SPACE(sizeof(struct ucred))];
   msg.msg_control = buf;
   msg.msg_controllen = sizeof buf;
   struct cmsghdr *cmsg;
   struct ucred *creds;

   cmsg = CMSG_FIRSTHDR(&msg);
   cmsg->cmsg_level = SOL_SOCKET;
   cmsg->cmsg_type = SCM_CREDENTIALS;
   cmsg->cmsg_len = CMSG_LEN(sizeof(struct ucred));
   /* Initialize the payload: */
   creds = (struct ucred *)CMSG_DATA(cmsg);
   creds->pid = getpid();
   creds->uid = getuid();
   creds->gid = getgid();
#endif

   int rv = sendmsg(sock, &msg, 0);
   if (-1 == rv) { handleerrno(env); }
}

/*
 * Class:     cx_ath_matthew_unix_UnixSocket
 * Method:    native_recv_creds
 * Signature: ([I)B
 */
JNIEXPORT jbyte JNICALL Java_cx_ath_matthew_unix_UnixSocket_native_1recv_1creds
  (JNIEnv *env, jobject o, jint sock, jintArray jcreds)
{
   struct msghdr msg;
   char iov_buf = 0;
   struct iovec iov;
   msg.msg_name = NULL;
   msg.msg_namelen = 0;
   msg.msg_flags = 0;
   msg.msg_iov = &iov;
   msg.msg_iovlen = 1;
   msg.msg_control = NULL;
   msg.msg_controllen = 0;
   iov.iov_base = &iov_buf;
   iov.iov_len = 1;

#ifdef SCM_CREDENTIALS
   char buf[CMSG_SPACE(sizeof(struct ucred))];
   msg.msg_control = buf;
   msg.msg_controllen = sizeof buf;
   struct cmsghdr *cmsg;
   struct ucred *creds = NULL;
#endif

   recvmsg(sock, &msg, 0);

#ifdef SCM_CREDENTIALS
   for (cmsg = CMSG_FIRSTHDR(&msg);
         cmsg != NULL;
         cmsg = CMSG_NXTHDR(&msg,cmsg)) {
      if (cmsg->cmsg_level == SOL_SOCKET
            && cmsg->cmsg_type == SCM_CREDENTIALS) {
         creds = (struct ucred *) CMSG_DATA(cmsg);        
         break;
      }
   }
   if (NULL != creds) {
      jint cred_array[3];
      cred_array[0] = creds->pid;
      cred_array[1] = creds->uid;
      cred_array[2] = creds->gid;
      (*env)->SetIntArrayRegion(env, jcreds, 0, 3, &cred_array[0]);
   }
#endif

   return iov_buf;
}


#ifdef __cplusplus
}
#endif
