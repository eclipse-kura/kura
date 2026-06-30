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
#if defined(NSLOGGING_AVAILABLE)
#define LOG(x) ivelog x
#else
#define LOG(x)
#endif
#define  J9_ERROR_ACCESS_DENIED 100
#define  J9_ERROR_FILE_NOT_FOUND 101
#define J9_UNKNOWN_ERROR 102
void ivelog(char *format, ...);
void iveSerThrow( JNIEnv *, char *, int );
void iveSerThrowWin( JNIEnv *, char *, int	);
void iveSerClearCommErrors( HANDLE );
