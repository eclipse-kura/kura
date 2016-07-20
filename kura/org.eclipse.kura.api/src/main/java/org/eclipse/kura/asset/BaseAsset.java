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
package org.eclipse.kura.asset;

import java.util.Map;

/**
 * The interface BaseAsset is a basic Kura asset of an Industrial Field Device
 * which associates a device driver. All devices which associates a driver
 * should use this for more specific functionalities to conform to the Kura
 * asset specifications.
 *
 * The basic asset requires a specific set of configurations to be provided by
 * the user. Please check {@see AssetConfiguration} for more information on how
 * to provide the configurations to the basic Kura asset.
 *
 * @see AssetConfiguration
 */
public interface BaseAsset extends Asset {

	/**
	 * Deinitializes the BaseDevice component by removing the expensive
	 * resources
	 */
	public void deinitialize();

	/**
	 * Gets the asset configuration.
	 *
	 * @return the asset configuration
	 */
	public AssetConfiguration getAssetConfiguration();

	/**
	 * Returns the injected instance of the Driver
	 *
	 * @return the injected driver instance
	 */
	public Driver getDriver();

	/**
	 * Initializes the BaseAsset component with the provided properties
	 *
	 * @param properties
	 *            the provided properties to parse
	 */
	public void initialize(Map<String, Object> properties);

	/**
	 * Updates the BaseDevice component with the newly provided properties
	 *
	 * @param properties
	 *            the updated properties to parse
	 */
	public void updated(Map<String, Object> properties);

}
