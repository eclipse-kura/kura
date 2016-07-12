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
import org.eclipse.kura.annotation.Nullable;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * The Class WireConfiguration represents a wiring configuration between a Wire
 * Emitter and a Wire Receiver
 */
@NotThreadSafe
public final class WireConfiguration {

	/** The Wire Emitter Name. */
	private String emitterName;

	/** The Filter. */
	@Nullable
	private final String filter;

	/**
	 * This signifies if Wire Admin Service has already created the wire between
	 * the wire emitter and the wire receiver.
	 */
	private boolean isCreated;

	/** The Wire Receiver Name. */
	private String receiverName;

	/**
	 * Instantiate a new wire configuration.
	 *
	 * @param emitterName
	 *            the Wire Emitter name
	 * @param receiverName
	 *            the Wire Receiver name
	 * @param filter
	 *            the filter
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null (except filter)
	 */
	public WireConfiguration(final String emitterName, final String receiverName, @Nullable final String filter) {
		checkNull(emitterName, "Emitter name cannot be null");
		checkNull(receiverName, "Receiver name cannot be null");

		this.emitterName = emitterName;
		this.receiverName = receiverName;
		this.filter = filter;
		this.isCreated = false;
	}

	/**
	 * Instantiate a new wire configuration.
	 *
	 * @param emitterName
	 *            the Wire Emitter name
	 * @param receiverName
	 *            the Wire Receiver name
	 * @param filter
	 *            the filter
	 * @param isCreated
	 *            the created flag signifying whether Wire Admin has already
	 *            created the wire between the wire emitter and a wire receiver
	 * @throws KuraRuntimeException
	 *             if any of the arguments is null (except filter)
	 */
	public WireConfiguration(final String emitterName, final String receiverName, @Nullable final String filter,
			final boolean isCreated) {
		checkNull(emitterName, "Emitter name cannot be null");
		checkNull(receiverName, "Receiver name cannot be null");

		this.emitterName = emitterName;
		this.receiverName = receiverName;
		this.filter = filter;
		this.isCreated = isCreated;
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof WireConfiguration) {
			final WireConfiguration wireConfiguration = (WireConfiguration) obj;
			return Objects.equal(this.emitterName, wireConfiguration.getEmitterName())
					&& Objects.equal(this.receiverName, wireConfiguration.getReceiverName())
					&& Objects.equal(this.isCreated, wireConfiguration.isCreated());
		}
		return false;
	}

	/**
	 * Get the Wire Emitter name.
	 *
	 * @return the Wire Emitter name
	 */
	public String getEmitterName() {
		return this.emitterName;
	}

	/**
	 * Get the filter.
	 *
	 * @return the filter
	 */
	public String getFilter() {
		return this.filter;
	}

	/**
	 * Get the Wire Receiver name.
	 *
	 * @return the Wire Receiver name
	 */
	public String getReceiverName() {
		return this.receiverName;
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		return Objects.hashCode(this.isCreated, this.emitterName, this.receiverName);
	}

	/**
	 * Check if is wire admin has already created a wire between the wire
	 * emitter and a wire receiver
	 *
	 * @return true, if it is created
	 */
	public boolean isCreated() {
		return this.isCreated;
	}

	/**
	 * Set the value to the flag to check whether wire admin has already created
	 * a wire between the wire emitter and a wire receiver
	 *
	 * @param isCreated
	 *            the new created
	 */
	public void setCreated(final boolean isCreated) {
		this.isCreated = isCreated;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("emitter_name", this.emitterName)
				.add("receiver_name", this.receiverName).add("filter", this.filter).toString();
	}

	/**
	 * Update the names of the Wire Components associated with this Wire
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
