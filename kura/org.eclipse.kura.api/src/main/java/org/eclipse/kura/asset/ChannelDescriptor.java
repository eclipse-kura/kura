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

import org.eclipse.kura.configuration.ConfigurableComponent;

/**
 * The Interface ChannelDescriptor is mainly used to provide the protocol
 * specific channel descriptions which will be used by the assets. It would
 * enable asset to provide its protocol specific configurable properties.
 */
public interface ChannelDescriptor {

	/**
	 * Returns the attribute definitions for the protocol specific channels.
	 * Currently Kura only supports {@code AD} to provide configurable
	 * properties for a {@link ConfigurableComponent}. Hence, it should return a
	 * list of {@code AD}. Some implementors can also provide custom objects. In
	 * such case, implementors must also provide another implementation for
	 * providing configurable properties of a {@link ConfigurableComponent}
	 * using the provided custom object. As currently the {@code AD} uses OSGi
	 * Configuration Admin service to provide configurable properties, the
	 * custom object class needs to use OSGi Configuration Admin service for
	 * that same purpose.
	 *
	 * @return the container of attribute definitions
	 */
	public Object getDescriptor();

}
