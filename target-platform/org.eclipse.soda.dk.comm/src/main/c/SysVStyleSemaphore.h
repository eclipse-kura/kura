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
#ifndef _SysVStyleSemaphore_h_
#define _SysVStyleSemaphore_h_
#include <semaphore.h>
#ifdef NCI
#define SEM_TBL_SIZE 5
#else
#define SEM_TBL_SIZE 8
#endif
typedef struct {
	sem_t * semaphore;
	int id;
	int pid; /* currently not used */
} sem_entry;
/* Looks up a POSIX semaphore by an integer identifyer.
	Used to circumvent sysV type semaphores */
sem_t* sem_lookup(int semID);															 
															 
/* Creates a POSIX semaphore and adds it to sem_tbl.
	Returns the semaphore id or -1 for error.*/
int sem_create(int semID, int initialSize);
#endif
