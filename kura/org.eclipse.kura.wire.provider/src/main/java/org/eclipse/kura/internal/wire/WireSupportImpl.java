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
package org.eclipse.kura.internal.wire;

import static org.eclipse.kura.Preconditions.checkNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.eclipse.kura.util.service.ServiceUtil;
import org.eclipse.kura.wire.SeverityLevel;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireField;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.wireadmin.Wire;

/**
 * The Class WireSupportImpl implements {@link WireSupport}
 */
final class WireSupportImpl implements WireSupport {

	/** Localization Resource */
	private static final WireMessages s_message = LocalizationAdapter.adapt(WireMessages.class);

	/** The incoming wires. */
	private List<Wire> m_incomingWires;

	/** The outgoing wires. */
	private List<Wire> m_outgoingWires;

	/** The Wire Helper Service. */
	private final WireHelperService m_wireHelperService;

	/** The supported Wire Component. */
	private final WireComponent m_wireSupporter;

	/**
	 * Instantiates a new wire support implementation.
	 *
	 * @param wireSupporter
	 *            the wire supporter
	 * @param wireHelperService
	 *            the Wire Helper service
	 */
	WireSupportImpl(final WireComponent wireSupporter, final WireHelperService wireHelperService) {
		checkNull(wireSupporter, s_message.wireSupportedComponentNonNull());
		checkNull(wireHelperService, s_message.wireHelperServiceNonNull());

		this.m_outgoingWires = CollectionUtil.newArrayList();
		this.m_incomingWires = CollectionUtil.newArrayList();
		this.m_wireSupporter = wireSupporter;
		this.m_wireHelperService = wireHelperService;
	}

	/** {@inheritDoc} */
	@Override
	public synchronized void consumersConnected(final Wire[] wires) {
		this.m_outgoingWires = Arrays.asList(wires);
	}

	/** {@inheritDoc} */
	@Override
	public synchronized void emit(final List<WireRecord> wireRecords) {
		checkNull(wireRecords, s_message.wireRecordsNonNull());
		if (this.m_wireSupporter instanceof WireEmitter) {
			final String emitterPid = this.m_wireHelperService.getServicePid(this.m_wireSupporter);
			final WireEnvelope wei = new WireEnvelope(emitterPid, wireRecords);
			for (final Wire wire : this.m_outgoingWires) {
				wire.update(wei);
			}
		}
	}

	/** {@inheritDoc} */
	@Override
	public List<WireRecord> filter(final List<WireRecord> records) {
		final SeverityLevel level = this.getSeverityLevel();
		if (level == null) {
			return records;
		}
		final List<WireRecord> newRecords = CollectionUtil.newArrayList();
		final List<WireField> newFields = CollectionUtil.newArrayList();
		for (final WireRecord wireRecord : records) {
			for (final WireField wireField : wireRecord.getFields()) {
				// If the severity level is info, then only info wire fields
				// will remain
				final SeverityLevel wireFieldLevel = wireField.getSeverityLevel();
				if ((wireFieldLevel == SeverityLevel.INFO) && (level == SeverityLevel.INFO)) {
					newFields.add(wireField);
				}
				// If the severity level is error, then all wire fields remain
				if (((wireFieldLevel == SeverityLevel.INFO) || (wireFieldLevel == SeverityLevel.CONFIG)
						|| (wireFieldLevel == SeverityLevel.ERROR)) && (level == SeverityLevel.ERROR)) {
					newFields.add(wireField);
				}
				// If the severity level is config, then info and config wire
				// fields remain
				if (((wireFieldLevel == SeverityLevel.INFO) || (wireFieldLevel == SeverityLevel.CONFIG))
						&& (level == SeverityLevel.CONFIG)) {
					newFields.add(wireField);
				}
				final WireRecord newWireRecord = new WireRecord(wireRecord.getTimestamp(), wireRecord.getPosition(),
						newFields);
				newRecords.add(newWireRecord);
			}
		}
		return newRecords;
	}

	/**
	 * Gets the incoming wires.
	 *
	 * @return the incoming wires
	 */
	List<Wire> getIncomingWires() {
		return Collections.unmodifiableList(this.m_incomingWires);
	}

	/**
	 * Gets the outgoing wires.
	 *
	 * @return the outgoing wires
	 */
	List<Wire> getOutgoingWires() {
		return Collections.unmodifiableList(this.m_outgoingWires);
	}

	/**
	 * Returns the severity level of the wire component
	 *
	 * @return the severity level
	 */
	private SeverityLevel getSeverityLevel() {
		final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
		final ServiceReference<WireComponent>[] refs = ServiceUtil.getServiceReferences(context, WireComponent.class,
				null);
		String property = null;
		for (final ServiceReference<WireComponent> ref : refs) {
			final WireComponent component = context.getService(ref);
			if (component == this.m_wireSupporter) {
				property = ref.getProperty("severity.level").toString();
				break;
			}
		}
		if ("INFO".equalsIgnoreCase(property)) {
			return SeverityLevel.INFO;
		}
		if ("ERROR".equalsIgnoreCase(property)) {
			return SeverityLevel.ERROR;
		}
		if ("CONFIG".equalsIgnoreCase(property)) {
			return SeverityLevel.CONFIG;
		}
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public synchronized Object polled(final Wire wire) {
		return wire.getLastValue();
	}

	/** {@inheritDoc} */
	@Override
	public void producersConnected(final Wire[] wires) {
		this.m_incomingWires = Arrays.asList(wires);
	}

	/** {@inheritDoc} */
	@Override
	public void updated(final Wire wire, final Object value) {
		checkNull(wire, s_message.wireNonNull());
		if ((value instanceof WireEnvelope) && (this.m_wireSupporter instanceof WireReceiver)) {
			((WireReceiver) this.m_wireSupporter).onWireReceive((WireEnvelope) value);
		}
	}
}
