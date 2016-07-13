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

import static org.eclipse.kura.Preconditions.checkNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.WireMessages;
import org.eclipse.kura.wire.WireConfiguration;
import org.eclipse.kura.wire.Wires;

import com.google.common.base.CharMatcher;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Captures the configuration of the Wire Graph.
 */
final class WireServiceOptions {

	/** Localization Resource */
	private static final WireMessages s_message = LocalizationAdapter.adapt(WireMessages.class);

	/** The list of wire configurations. */
	private final List<WireConfiguration> wireConfigurations;

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
		this.wireConfigurations = configurations;
	}
	
	/**
	 * Creates new instance of {@link WireServiceOptions}
	 *
	 * @param properties
	 *            the properties
	 * @return the wire service options
	 * @throws KuraRuntimeException
	 *             if provided properties is null
	 */
	static WireServiceOptions getInstance(final Map<String, Object> properties) {
		checkNull(properties, s_message.wireServicePropNonNull());
		final List<WireConfiguration> wireConfs = Lists.newCopyOnWriteArrayList();
		final Set<Long> wireIds = Sets.newHashSet();
		final String separator = ".";
		for (final Map.Entry<String, Object> entry : properties.entrySet()) {
			final String key = entry.getKey();
			if (key.contains(separator)) {
				final Long wireConfId = Long.parseLong(key.substring(0, key.indexOf(separator)));
				wireIds.add(wireConfId);
			}
		}
		for (int i = 0; i < wireIds.size(); i++) {
			String emitterName = null;
			String receiverName = null;
			String filter = null;
			for (final Map.Entry<String, Object> entry : properties.entrySet()) {
				final String key = entry.getKey();
				final String value = String.valueOf(entry.getValue());

				if (CharMatcher.DIGIT.matchesAllOf(key.substring(0, key.indexOf(separator)))) {
					if (key.contains("emitter")) {
						emitterName = value;
					}
					if (key.contains("receiver")) {
						receiverName = value;
					}
					if (key.contains("filter")) {
						filter = value;
					}
				}
			}
			final WireConfiguration configuration = Wires.newWireConfiguration(emitterName, receiverName, filter);
			wireConfs.add(configuration);
		}

		return new WireServiceOptions(wireConfs);
	}

	/**
	 * Gets the wire configurations.
	 *
	 * @return the wire configurations
	 */
	List<WireConfiguration> getWireConfigurations() {
		return ImmutableList.copyOf(this.wireConfigurations);
	}

	/**
	 * Gets the wire configurations
	 *
	 * @return the wire configurations
	 */
	List<WireConfiguration> getWires() {
		return this.wireConfigurations;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add(s_message.wireConf(), this.wireConfigurations).toString();
	}

}
