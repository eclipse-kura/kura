/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.wire;

import org.eclipse.kura.KuraRuntimeException;

/**
 * The Interface WireRecordStore is responsible to store and manage wire records
 * in a dedicated record store
 */
public interface WireRecordStore {

	/**
	 * Clear all the stored wired records from the provided table.
	 *
	 * @param tableName
	 *            the table name whose records will be truncated
	 * @throws KuraRuntimeException
	 *             if the argument is null
	 */
	public void clear(String tableName);

	/**
	 * Stores the provided wire record into the provided table.
	 *
	 * @param tableName
	 *            the table name to which the wire record will be inserted
	 * @param wireRecord
	 *            the wire record to insert
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null
	 */
	public void store(String tableName, WireRecord wireRecord);

}
