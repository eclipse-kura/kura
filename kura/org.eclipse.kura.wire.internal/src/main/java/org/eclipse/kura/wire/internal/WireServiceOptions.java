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

import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraRuntimeException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Immutable object to capture the configuration of the Wire Graph.
 */
public final class WireServiceOptions {

	/** The Constant denoting the wires. */
	public static final String CONF_WIRES = "wires";

	/** The list of wire configurations. */
	private static List<WireConfiguration> m_wireConfigurations;

	/**
	 * New instance of {@link WireServiceOptions}
	 *
	 * @param properties
	 *            the properties
	 * @return the wire service options
	 * @throws JSONException
	 *             the JSON exception
	 * @throws KuraRuntimeException
	 *             if provided properties is null
	 */
	public static WireServiceOptions newInstance(final Map<String, Object> properties) throws JSONException {
		checkCondition(properties == null, "Configured Wire Service properties cannot be null");

		final List<WireConfiguration> wireConfs = Lists.newCopyOnWriteArrayList();
		Object objWires = null;
		if (properties.containsKey(CONF_WIRES)) {
			objWires = properties.get(CONF_WIRES);
		}
		if (objWires instanceof String) {
			final String strWires = (String) objWires;
			final JSONArray jsonWires = new JSONArray(strWires);
			for (int i = 0; i < jsonWires.length(); i++) {
				final JSONObject jsonWire = jsonWires.getJSONObject(i);
				wireConfs.add(WireConfiguration.newInstanceFromJson(jsonWire));
			}
		}
		return new WireServiceOptions(wireConfs);
	}

	/**
	 * Instantiates a new wire service options.
	 *
	 * @param configurations
	 *            the list of Wire Configurations
	 * @throws KuraRuntimeException
	 *             if provided configurations is null
	 */
	private WireServiceOptions(final List<WireConfiguration> configurations) {
		checkCondition(configurations == null, "Configurations cannot be null");
		m_wireConfigurations = configurations;
	}

	/**
	 * Gets the wire configurations.
	 *
	 * @return the wire configurations
	 */
	public List<WireConfiguration> getWireConfigurations() {
		return ImmutableList.copyOf(m_wireConfigurations);
	}

	/**
	 * Gets the wire configurations
	 *
	 * @return the wire configurations
	 */
	public List<WireConfiguration> getWires() {
		return m_wireConfigurations;
	}

	/**
	 * Converts the Wire Configuration to json string.
	 *
	 * @return the string in json format
	 * @throws JSONException
	 *             the JSON exception
	 */
	public String toJsonString() throws JSONException {
		final JSONArray jsonWires = new JSONArray();
		for (final WireConfiguration wireConfig : m_wireConfigurations) {
			jsonWires.put(wireConfig.toJson());
		}
		return jsonWires.toString();
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("wire_configurations", m_wireConfigurations).toString();
	}

}
