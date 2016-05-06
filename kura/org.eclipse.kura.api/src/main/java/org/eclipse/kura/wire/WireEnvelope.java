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

import java.util.List;

import org.osgi.service.wireadmin.BasicEnvelope;

import com.google.common.base.MoreObjects;

/**
 * The Class WireEnvelope represents a composite envelope to be used as an
 * abstract data to be transmitted between the wire emitter and the wire
 * receiver
 */
public final class WireEnvelope extends BasicEnvelope {

	/**
	 * Instantiates a new wire envelope.
	 *
	 * @param emitterName
	 *            the wire emitter name
	 * @param wireRecords
	 *            the wire records
	 */
	public WireEnvelope(final String emitterName, final List<WireRecord> wireRecords) {
		super(wireRecords, emitterName, null);
	}

	/**
	 * Gets the wire emitter name.
	 *
	 * @return the wire emitter name
	 */
	public String getEmitterName() {
		return (String) this.getIdentification();
	}

	/**
	 * Gets the wire records.
	 *
	 * @return the wire records
	 */
	@SuppressWarnings("unchecked")
	public List<WireRecord> getRecords() {
		return (List<WireRecord>) this.getValue();
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("emitter_name", this.getEmitterName())
				.add("wire_records", this.getRecords()).toString();
	}
}
