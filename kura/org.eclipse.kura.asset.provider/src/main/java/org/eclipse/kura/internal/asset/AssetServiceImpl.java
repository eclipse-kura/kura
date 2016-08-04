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
package org.eclipse.kura.internal.asset;

import org.eclipse.kura.asset.Asset;
import org.eclipse.kura.asset.AssetService;

/**
 * The Class AssetServiceImpl is an implementation of the utility API
 * {@link AssetService} to provide useful factory methods for assets
 */
public final class AssetServiceImpl implements AssetService {

	/** {@inheritDoc} */
	@Override
	public Asset newAsset() {
		return new AssetImpl();
	}

}
