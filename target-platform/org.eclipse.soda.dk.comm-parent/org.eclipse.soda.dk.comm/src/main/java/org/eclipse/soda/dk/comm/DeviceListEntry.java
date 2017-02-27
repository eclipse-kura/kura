package org.eclipse.soda.dk.comm;

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
/**
 * @author IBM
 * @version 1.2.0
 * @since 1.0
 */
public class DeviceListEntry {
	/**
	 * Define the logical name (String) field.
	 */
	String logicalName;

	/**
	 * Define the physical name (String) field.
	 */
	String physicalName;

	/**
	 * Define the port type (int) field.
	 */
	int portType;

	/**
	 * Define the sem id (int) field.
	 */
	int semID;

	/**
	 * Define the opened (boolean) field.
	 */
	boolean opened;

	/**
	 * Define the next (DeviceListEntry) field.
	 */
	DeviceListEntry next;
}
