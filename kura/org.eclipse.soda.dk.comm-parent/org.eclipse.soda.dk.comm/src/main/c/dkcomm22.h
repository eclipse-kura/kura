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
union semuni {
  int val;                   /* value for SETVAL             */
  struct semid_ds *buf;      /* buffer for IPC_STAT, IPC_SET */
  unsigned short int *array; /* array for GETALL, SETALL     */
  struct seminfo *__buf;     /* buffer for IPC_INFO          */
};
