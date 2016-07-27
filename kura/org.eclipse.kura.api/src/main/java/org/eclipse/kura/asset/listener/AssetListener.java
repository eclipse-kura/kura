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
package org.eclipse.kura.asset.listener;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.asset.AssetEvent;

/**
 * The listener interface AssetListener is mainly for receiving asset events.
 * The class that is interested in processing a asset event implements this
 * interface, and the object created with that class is registered with a asset
 * component using the asset component's
 * {@code Asset#registerAssetListener(String, AssetListener)} method. When the
 * asset event occurs, that object's appropriate method is invoked.
 *
 * @see AssetEvent
 */
public interface AssetListener {

	/**
	 * Triggers on asset event
	 *
	 * @param event
	 *            the fired asset event
	 * @throws KuraRuntimeException
	 *             if event is null
	 */
	public void onAssetEvent(AssetEvent event);

}
