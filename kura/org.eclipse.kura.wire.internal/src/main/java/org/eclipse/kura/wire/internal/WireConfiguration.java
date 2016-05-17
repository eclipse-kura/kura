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

package org.eclipse.kura.wire.internal;

import static org.eclipse.kura.device.internal.Preconditions.checkCondition;

import org.eclipse.kura.KuraRuntimeException;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.MoreObjects;

/**
 * The Class WireConfiguration represents a wiring configuration between a Wire
 * Emitter and a Wire Receiver
 */
public final class WireConfiguration {

	/**
	 * Returns new instance of Wire Configuration from json object provided
	 *
	 * @param jsonWire
	 *            the json object representing the wires
	 * @return the wire configuration
	 * @throws JSONException
	 *             the JSON exception
	 * @throws KuraRuntimeException
	 *             if the json object instance passed as argument is null
	 */
	public static WireConfiguration newInstanceFromJson(final JSONObject jsonWire) throws JSONException {
		checkCondition(jsonWire == null, "JSON Object cannot be null");

		final String emitter = jsonWire.getString("p");
		final String receiver = jsonWire.getString("c");
		final String filter = jsonWire.optString("f");
		return new WireConfiguration(emitter, receiver, filter);
	}

	/**
	 * This signifies if wire admin has already created the wire between the
	 * wire emitter and the wire receiver.
	 */
	private boolean m_created = false;

	/** The Wire Emitter Name. */
	private String m_emitterName;

	/** The Filter. */
	private final String m_filter;

	/** The Wire Receiver Name. */
	private String m_receiverName;

	/**
	 * Instantiates a new wire configuration.
	 *
	 * @param emitterName
	 *            the Wire Emitter name
	 * @param receiverName
	 *            the Wire Receiver name
	 * @param filter
	 *            the filter
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null
	 */
	public WireConfiguration(final String emitterName, final String receiverName, final String filter) {
		checkCondition(emitterName == null, "Emitter name cannot be null");
		checkCondition(receiverName == null, "Receiver name cannot be null");
		checkCondition(filter == null, "Filter cannot be null");

		this.m_emitterName = emitterName;
		this.m_receiverName = receiverName;
		this.m_filter = filter;
	}

	/**
	 * Instantiates a new wire configuration.
	 *
	 * @param emitterName
	 *            the Wire Emitter name
	 * @param receiverName
	 *            the Wire Receiver name
	 * @param filter
	 *            the filter
	 * @param created
	 *            the created flag signifying whether Wire Admin has already
	 *            created the wire between the wire emitter and a wire receiver
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null
	 */
	public WireConfiguration(final String emitterName, final String receiverName, final String filter,
			final boolean created) {
		checkCondition(emitterName == null, "Emitter name cannot be null");
		checkCondition(receiverName == null, "Receiver name cannot be null");
		checkCondition(filter == null, "Filter cannot be null");

		this.m_emitterName = emitterName;
		this.m_receiverName = receiverName;
		this.m_filter = filter;
		this.m_created = created;
	}

	/**
	 * Gets the Wire Emitter name.
	 *
	 * @return the Wire Emitter name
	 */
	public String getEmitterName() {
		return this.m_emitterName;
	}

	/**
	 * Gets the filter.
	 *
	 * @return the filter
	 */
	public String getFilter() {
		return this.m_filter;
	}

	/**
	 * Gets the Wire Receiver name.
	 *
	 * @return the Wire Receiver name
	 */
	public String getReceiverName() {
		return this.m_receiverName;
	}

	/**
	 * Checks if is wire admin has already created a wire between the wire
	 * emitter and a wire receiver
	 *
	 * @return true, if it is created
	 */
	public boolean isCreated() {
		return this.m_created;
	}

	/**
	 * Sets the value to the flag to check whether wire admin has already
	 * created a wire between the wire emitter and a wire receiver
	 *
	 * @param created
	 *            the new created
	 */
	public void setCreated(final boolean created) {
		this.m_created = created;
	}

	/**
	 * Converts the Wire Configuration to json.
	 *
	 * @return the JSON object
	 * @throws JSONException
	 *             the JSON exception
	 */
	public JSONObject toJson() throws JSONException {
		final JSONObject jsonWire = new JSONObject();
		jsonWire.put("p", this.m_emitterName);
		jsonWire.put("c", this.m_receiverName);
		if ((this.m_filter != null) && !this.m_filter.isEmpty()) {
			jsonWire.putOpt("f", this.m_filter);
		}
		return jsonWire;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("emitter_name", this.m_emitterName)
				.add("receiver_name", this.m_receiverName).add("filter", this.m_filter).toString();
	}

	/**
	 * Updates the names of the Wire Components associated with this Wire
	 * Configuration
	 *
	 * @param newEmitterName
	 *            the new Wire Emitter name
	 * @param newReceiverName
	 *            the new Wire Receiver name
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null
	 */
	public void update(final String newEmitterName, final String newReceiverName) {
		checkCondition(newEmitterName == null, "Emitter name cannot be null");
		checkCondition(newReceiverName == null, "Receiver name cannot be null");

		this.m_emitterName = newEmitterName;
		this.m_receiverName = newReceiverName;
	}
}
