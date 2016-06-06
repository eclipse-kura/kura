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
package org.eclipse.kura.wire.store;

import java.util.Map;

import org.eclipse.kura.wire.internal.AbstractConfigurationOptions;

/**
 * The Class DbWireRecordStoreOptions is responsible to contain all the DB Wire
 * Record Store related options
 */
public final class DbWireRecordStoreOptions extends AbstractConfigurationOptions {

	/**
	 * Instantiates a new DB wire record store options.
	 *
	 * @param properties
	 *            the configured properties
	 */
	public DbWireRecordStoreOptions(final Map<String, Object> properties) {
		super(properties);
	}

}
