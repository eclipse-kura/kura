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

/**
 * The Class DbWireRecordStoreOptions is responsible to contain all the DB Wire
 * Record Store related options
 */
public final class DbWireRecordStoreOptions {

	/** The Constant denotes wire emitter. */
	private static final String CONF_EMITTER_ID = "emitter.id";

	/** The Constant denotes wire receiver. */
	private static final String CONF_EMITTERS = "data.emitters";

	/** The m_properties. */
	private final Map<String, Object> m_properties;

	/**
	 * Instantiates a new DB wire record store options.
	 *
	 * @param properties
	 *            the configured properties
	 */
	public DbWireRecordStoreOptions(final Map<String, Object> properties) {
		this.m_properties = properties;
	}

	/**
	 * Returns the ID to be used for this wire emitter.
	 *
	 * @return the emitter id
	 */
	public String getEmitterId() {
		String emitterId = null;
		if ((this.m_properties != null) && (this.m_properties.get(CONF_EMITTER_ID) != null)
				&& (this.m_properties.get(CONF_EMITTER_ID) instanceof String)) {
			emitterId = (String) this.m_properties.get(CONF_EMITTER_ID);
		}
		return emitterId;
	}

	/**
	 * Returns the wire emitters to be used for message publishing.
	 *
	 * @return the subscribed emitters
	 */
	public String[] getSubscribedEmitters() {
		String[] emitteres = {};
		if ((this.m_properties != null) && (this.m_properties.get(CONF_EMITTERS) != null)
				&& (this.m_properties.get(CONF_EMITTERS) instanceof String)) {
			final String emittersStr = (String) this.m_properties.get(CONF_EMITTERS);
			emitteres = emittersStr.split(",");
		}
		return emitteres;
	}
}
