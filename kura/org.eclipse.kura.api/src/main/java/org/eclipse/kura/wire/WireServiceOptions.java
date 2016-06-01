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

import java.util.List;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.annotation.Immutable;
import org.eclipse.kura.annotation.ThreadSafe;
import org.json.JSONArray;
import org.json.JSONException;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

/**
 * Immutable object to capture the configuration of the Wire Graph.
 */
@Immutable
@ThreadSafe
public final class WireServiceOptions {

	/** The Constant denoting the wires. */
	public static final String CONF_WIRES = "wires";

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
	public WireServiceOptions(final List<WireConfiguration> configurations) {
		checkNull(configurations, "Configurations cannot be null");
		this.wireConfigurations = configurations;
	}

	/**
	 * Gets the wire configurations.
	 *
	 * @return the wire configurations
	 */
	public List<WireConfiguration> getWireConfigurations() {
		return ImmutableList.copyOf(this.wireConfigurations);
	}

	/**
	 * Gets the wire configurations
	 *
	 * @return the wire configurations
	 */
	public List<WireConfiguration> getWires() {
		return this.wireConfigurations;
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
		for (final WireConfiguration wireConfig : this.wireConfigurations) {
			jsonWires.put(wireConfig.toJson());
		}
		return jsonWires.toString();
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("wire_configurations", this.wireConfigurations).toString();
	}

}
