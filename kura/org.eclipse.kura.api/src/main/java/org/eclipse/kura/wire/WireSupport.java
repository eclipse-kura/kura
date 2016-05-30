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

import org.osgi.service.wireadmin.Consumer;
import org.osgi.service.wireadmin.Producer;
import org.osgi.service.wireadmin.Wire;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * The Class WireSupport is responsible for managing incoming as well as
 * outgoing wires of the contained Wire Component. This is also used to perform
 * wire related operations for instance, emit and receive wire records
 */
public final class WireSupport implements Producer, Consumer {

	/** The incoming wires. */
	private List<Wire> m_incomingWires;

	/** The outgoing wires. */
	private List<Wire> m_outgoingWires;

	/** The supported Wire Component */
	private final WireComponent m_wireSupporter;

	/**
	 * Instantiates a new wire support.
	 *
	 * @param wireSupporter
	 *            the wire supporter component
	 */
	public WireSupport(final WireComponent wireSupporter) {
		this.m_outgoingWires = Lists.newArrayList();
		this.m_incomingWires = Lists.newArrayList();
		this.m_wireSupporter = wireSupporter;
	}

	/** {@inheritDoc} */
	@Override
	public synchronized void consumersConnected(final Wire[] wires) {
		this.m_outgoingWires = Lists.newArrayList(wires);
	}

	/**
	 * Emit the provided wire records
	 *
	 * @param wireRecords
	 *            the wire records
	 */
	public synchronized void emit(final List<WireRecord> wireRecords) {
		if (this.m_wireSupporter instanceof WireEmitter) {
			final String emitterPid = this.m_wireSupporter.getName();
			final WireEnvelope wei = new WireEnvelope(emitterPid, wireRecords);
			for (final Wire wire : this.m_outgoingWires) {
				wire.update(wei);
			}
		}
	}

	/**
	 * Emit the provided wire records
	 *
	 * @param wireRecords
	 *            the wire records to be emitted
	 */
	public synchronized void emit(final WireRecord... wireRecords) {
		this.emit(Lists.newArrayList(wireRecords));
	}

	/**
	 * Gets the incoming wires.
	 *
	 * @return the incoming wires
	 */
	public List<Wire> getIncomingWires() {
		return ImmutableList.copyOf(this.m_incomingWires);
	}

	/**
	 * Gets the outgoing wires.
	 *
	 * @return the outgoing wires
	 */
	public List<Wire> getOutgoingWires() {
		return ImmutableList.copyOf(this.m_outgoingWires);
	}

	/** {@inheritDoc} */
	@Override
	public synchronized Object polled(final Wire wire) {
		return wire.getLastValue();
	}

	/** {@inheritDoc} */
	@Override
	public void producersConnected(final Wire[] wires) {
		this.m_incomingWires = Lists.newArrayList(wires);
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("incoming_wires", this.m_incomingWires)
				.add("outgoing_wires", this.m_outgoingWires).add("wire_component", this.m_wireSupporter).toString();
	}

	/** {@inheritDoc} */
	@Override
	public void updated(final Wire wire, final Object value) {
		if ((value instanceof WireEnvelope) && (this.m_wireSupporter instanceof WireReceiver)) {
			((WireReceiver) this.m_wireSupporter).onWireReceive((WireEnvelope) value);
		}
	}
}
