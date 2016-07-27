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
 * The interface Asset is a high level abstraction of a Kura asset of an
 * Industrial Field Device which associates a device driver. All devices which
 * associates a driver should use this for more specific functionalities to
 * conform to the Kura asset specifications.
 *
 * The asset requires a specific set of configurations to be provided by the
 * user. Please check {@see AssetConfiguration} for more information on how to
 * provide the configurations to the basic Kura asset.
 *
 * @see AssetConfiguration
 */
public interface Asset extends BaseAsset {

	/**
	 * Gets the asset configuration.
	 *
	 * @return the asset configuration
	 */
	public AssetConfiguration getAssetConfiguration();

	/**
	 * Initializes the Base Asset component with the provided properties
	 *
	 * @param properties
	 *            the provided properties to parse
	 */
	public void initialize(Map<String, Object> properties);

	/**
	 * Releases the expensive resources
	 */
	public void release();

}
