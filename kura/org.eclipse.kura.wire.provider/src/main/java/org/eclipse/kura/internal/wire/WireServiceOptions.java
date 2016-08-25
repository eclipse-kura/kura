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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.WireMessages;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.eclipse.kura.wire.WireConfiguration;

/**
 * Captures the configuration of the Wire Graph.
 */
final class WireServiceOptions {

	/** Regular Expression pattern used for checking wire configurations */
	private static final String PATTERN = "%s.";

	/** Localization Resource */
	private static final WireMessages s_message = LocalizationAdapter.adapt(WireMessages.class);

	/** The list of wire configurations. */
	private final List<WireConfiguration> m_wireConfigurations;

	/**
	 * Instantiates a new wire service options.
	 *
	 * @param configurations
	 *            the list of Wire Configurations
	 * @throws KuraRuntimeException
	 *             if provided configurations is null
	 */
	private WireServiceOptions(final List<WireConfiguration> configurations) {
		checkNull(configurations, s_message.configurationNonNull());
		checkNull(configurations, s_message.wireHelperServiceNonNull());

		this.m_wireConfigurations = configurations;
	}

	/**
	 * Gets the wire configurations.
	 *
	 * @return the wire configurations
	 */
	List<WireConfiguration> getWireConfigurations() {
		return this.m_wireConfigurations;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return "WireServiceOptions [m_wireConfigurations=" + this.m_wireConfigurations + "]";
	}

	/**
	 * Creates new instance of {@link WireServiceOptions}
	 *
	 * @param properties
	 *            the properties
	 * @param helperService
	 *            the Wire Helper Service instance
	 * @return the wire service options
	 * @throws KuraRuntimeException
	 *             if provided properties is null
	 */
	static WireServiceOptions getInstance(final Map<String, Object> properties) {
		checkNull(properties, s_message.wireServicePropNonNull());
		final List<WireConfiguration> wireConfs = CollectionUtil.newCopyOnWriteArrayList();
		final Set<Long> wireIds = CollectionUtil.newHashSet();
		final String separator = ".";
		for (final Map.Entry<String, Object> entry : properties.entrySet()) {
			final String key = entry.getKey();
			if (key.contains(separator) && Character.isDigit(key.charAt(0))) {
				final Long wireConfId = Long.parseLong(key.substring(0, key.indexOf(separator)));
				wireIds.add(wireConfId);
			}
		}
		final Iterator<Long> it = wireIds.iterator();
		while (it.hasNext()) {
			final String wireConfId = String.valueOf(it.next());
			String emitterPid = null;
			String receiverPid = null;
			String filter = null;
			for (final Map.Entry<String, Object> entry : properties.entrySet()) {
				final String key = entry.getKey();
				final String value = String.valueOf(entry.getValue());

				if (!key.contains(separator)) {
					continue;
				}
				if ((key.startsWith(String.format(PATTERN, wireConfId)))) {
					if (key.contains("emitter")) {
						emitterPid = value;
					}
					if (key.contains("receiver")) {
						receiverPid = value;
					}
					if (key.contains("filter")) {
						filter = value;
					}
				}
			}
			final WireConfiguration configuration = new WireConfiguration(emitterPid, receiverPid, filter);
			wireConfs.add(configuration);
		}
		return new WireServiceOptions(wireConfs);
	}

}
