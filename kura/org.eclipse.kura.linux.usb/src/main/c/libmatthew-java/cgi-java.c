/*
 * Java CGI Library
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

#include <jni.h>
#include "cgi-java.h"
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

extern char **environ;

extern jobjectArray Java_cx_ath_matthew_cgi_CGI_getfullenv (JNIEnv *env, jobject obj, jclass type)
{
   int i;
   for (i = 0; environ[i]; i++);
   jobjectArray joa =  (*env)->NewObjectArray(env, i+1, type, NULL);
   for (i = 0; environ[i]; i++)
      (*env)->SetObjectArrayElement(env, joa, i, (*env)->NewStringUTF(env, environ[i]));
   return joa;   
}

extern jstring Java_cx_ath_matthew_cgi_CGI_getenv (JNIEnv *env, jobject obj, jstring ename)
{
   const char *estr = (*env)->GetStringUTFChars(env, ename, 0);
   char *eval = getenv(estr);
   (*env)->ReleaseStringUTFChars(env, ename, estr);
   if (NULL == eval)
      return NULL;
   else
      return (*env)->NewStringUTF(env, eval);
}

extern void Java_cx_ath_matthew_cgi_CGI_setenv (JNIEnv *env, jobject obj, jstring var, jstring val)
{
#ifdef setenv
   const char *cvar = (*env)->GetStringUTFChars(env, var, 0);
   const char *cval = (*env)->GetStringUTFChars(env, val, 0);
   setenv(cvar, cval, 1);
#endif
}
