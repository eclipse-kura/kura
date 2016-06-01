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

import static org.eclipse.kura.Preconditions.checkNull;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.annotation.NotThreadSafe;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * The Class WireConfiguration represents a wiring configuration between a Wire
 * Emitter and a Wire Receiver
 */
@NotThreadSafe
public final class WireConfiguration {

	/**
	 * This signifies if Wire Admin Service has already created the wire between
	 * the wire emitter and the wire receiver.
	 */
	private boolean created;

	/** The Wire Emitter Name. */
	private String emitterName;

	/** The Filter. */
	private final String filter;

	/** The Wire Receiver Name. */
	private String receiverName;

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
		checkNull(emitterName, "Emitter name cannot be null");
		checkNull(receiverName, "Receiver name cannot be null");
		checkNull(filter, "Filter cannot be null");

		this.emitterName = emitterName;
		this.receiverName = receiverName;
		this.filter = filter;
		this.created = false;
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
		checkNull(emitterName, "Emitter name cannot be null");
		checkNull(receiverName, "Receiver name cannot be null");
		checkNull(filter, "Filter cannot be null");

		this.emitterName = emitterName;
		this.receiverName = receiverName;
		this.filter = filter;
		this.created = created;
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof WireConfiguration) {
			final WireConfiguration wireConfiguration = (WireConfiguration) obj;
			return Objects.equal(this.emitterName, wireConfiguration.getEmitterName())
					&& Objects.equal(this.receiverName, wireConfiguration.getReceiverName())
					&& Objects.equal(this.filter, wireConfiguration.getFilter())
					&& Objects.equal(this.created, wireConfiguration.isCreated());
		}
		return false;
	}

	/**
	 * Gets the Wire Emitter name.
	 *
	 * @return the Wire Emitter name
	 */
	public String getEmitterName() {
		return this.emitterName;
	}

	/**
	 * Gets the filter.
	 *
	 * @return the filter
	 */
	public String getFilter() {
		return this.filter;
	}

	/**
	 * Gets the Wire Receiver name.
	 *
	 * @return the Wire Receiver name
	 */
	public String getReceiverName() {
		return this.receiverName;
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		return Objects.hashCode(this.created, this.emitterName, this.filter, this.receiverName);
	}

	/**
	 * Checks if is wire admin has already created a wire between the wire
	 * emitter and a wire receiver
	 *
	 * @return true, if it is created
	 */
	public boolean isCreated() {
		return this.created;
	}

	/**
	 * Sets the value to the flag to check whether wire admin has already
	 * created a wire between the wire emitter and a wire receiver
	 *
	 * @param created
	 *            the new created
	 */
	public void setCreated(final boolean created) {
		this.created = created;
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
		jsonWire.put("p", this.emitterName);
		jsonWire.put("c", this.receiverName);
		if ((this.filter != null) && !this.filter.isEmpty()) {
			jsonWire.putOpt("f", this.filter);
		}
		return jsonWire;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("emitter_name", this.emitterName)
				.add("receiver_name", this.receiverName).add("filter", this.filter).toString();
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
		checkNull(newEmitterName, "Emitter name cannot be null");
		checkNull(newReceiverName, "Receiver name cannot be null");

		this.emitterName = newEmitterName;
		this.receiverName = newReceiverName;
	}
}
