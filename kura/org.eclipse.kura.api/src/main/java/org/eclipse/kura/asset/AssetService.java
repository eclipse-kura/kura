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

/**
 * The interface AssetService is an utility service API to provide useful
 * methods for assets
 */
public interface AssetService {

	/**
	 * Prepares the new asset instance
	 *
	 * @return the newly created Asset instance
	 */
	public Asset newAsset();

}
