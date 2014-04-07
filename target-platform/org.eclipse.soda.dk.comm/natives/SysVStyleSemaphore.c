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
#include "SysVStyleSemaphore.h"
/* created to allow sysV type lookup of semaphore by a unique integer ID
*/
static int sem_count = 0;
static sem_entry sem_tbl[SEM_TBL_SIZE];
/* Looks up a POSIX semaphore by an integer identifyer.
	Used to circumvent sysV type semaphores */
sem_t* sem_lookup(int semID) {
   return &(sem_tbl[semID].semaphore);
}
/* Creates a POSIX semaphore and adds it to sem_tbl.
	Returns the semaphore id or -1 for error.*/
int sem_create(int semID, int initialSize) {
   
	int i;
   
	for(i = 0; i < SEM_TBL_SIZE; i++){
      if(i >= sem_count) {
			/* allocate space fo semaphore */
			sem_count++;									  /* increment count */ 
			sem_init(sem_lookup(i), 0, initialSize); /* init semaphore */
			sem_tbl[i].id = semID; 					     /* set the id */
         return i;
		} else if(sem_tbl[i].id == semID){
         return i;
		}
   }
   
	return -1;
}
