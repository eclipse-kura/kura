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
#include <string.h>
#include <stdlib.h>
#include <time.h>
#include <windows.h>
#include "org_eclipse_soda_dk_comm_NSSerialPort.h"
#include "NSCommLOG.h"
/*------------------------------------------------------------------
 * 
 *------------------------------------------------------------------*/
#if 0 
void ivelog(char *format, ...) {
	va_list    varArgs;
	FILE      *file;
	time_t     timeNow;
	struct tm *tmNow;
	char       timeString[32];
	char      *logFileName;
	logFileName = "c:\\projects\\javaxcomm_win\\iveser.log";
//	file = fopen(logFileName,"w");
	file = fopen(logFileName,"a+w");
	if (NULL == file) return;
	timeNow = time(NULL);
	tmNow   = localtime(&timeNow);
	strftime(timeString,sizeof(timeString)-1,"%Y/%m/%d %H:%M:%S",tmNow);
	fprintf(file,"%s : ",timeString);
	va_start(varArgs,format);
	vfprintf(file,format,varArgs);
	va_end(varArgs);
	fprintf(file,"\n");
	fclose(file);
}
#endif
/*------------------------------------------------------------------
 * throw an exception
 *------------------------------------------------------------------*/
void iveSerThrow( JNIEnv *env, char *message, int rc ) {
	jclass clazz;
	
	LOG(("iveSerThrow(%s)",message));
	switch (rc){
		case J9_ERROR_ACCESS_DENIED:
			clazz = (*env)->FindClass(env, "javax/comm/PortInUseException");
			break;
		case J9_ERROR_FILE_NOT_FOUND:
			clazz = (*env)->FindClass(env, "javax/comm/NoSuchPortException");		
			break;
		default:
			clazz = (*env)->FindClass(env, "javax/comm/SerialPortException");			
	}
	if (!clazz) {
		LOG(("couldn't find exception class"));
		return;
	}
	(*env)->ThrowNew(env,clazz,message);
}
/*------------------------------------------------------------------
 * 
 *------------------------------------------------------------------*/
void iveSerThrowWin(
	JNIEnv *env, 
	char   *msg,
	int     rc
	) {
	char message[256];
	char rcBuffer[128];
	
	LOG(("iveSerThrowWin(%s,%d)",msg,rc));
	FormatMessage(FORMAT_MESSAGE_FROM_SYSTEM,0,rc,0,rcBuffer,sizeof(rcBuffer)-1,0);
	sprintf(message,"%s; rc=%d, %s",msg,rc,rcBuffer);
	
	switch (rc){
		case ERROR_ACCESS_DENIED:
			iveSerThrow(env,message, J9_ERROR_ACCESS_DENIED);
			break;
		case ERROR_FILE_NOT_FOUND:
			iveSerThrow(env,message, J9_ERROR_FILE_NOT_FOUND);
			break;
		default:
			iveSerThrow(env,message, J9_UNKNOWN_ERROR);
			break;
			
	}
}
/*------------------------------------------------------------------
 * clear comm errors
 *------------------------------------------------------------------*/
void iveSerClearCommErrors(
	HANDLE osHandle
	) {
	COMSTAT comStat;
	DWORD   dwErrors;
	ClearCommError(osHandle,&dwErrors,&comStat);
}
