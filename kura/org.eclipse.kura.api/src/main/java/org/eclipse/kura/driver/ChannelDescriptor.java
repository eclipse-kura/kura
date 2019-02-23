/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.eclipse.kura.driver;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The Interface ChannelDescriptor is mainly used to provide the protocol
 * specific channel descriptions which will be used by the assets in the Kura
 * Wires Visualization model. It would enable asset to provide its protocol
 * specific configurable properties.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 1.2
 */
@ProviderType
public interface ChannelDescriptor {

    /**
     * Returns the protocol specific descriptor. For using drivers in
     * cooperation with Kura Wires Visualization, the implementors can return
     * list of {@code AD}s to provide configurable properties for a
     * {@link ConfigurableComponent}.<br/>
     * <br/>
     *
     * This method is essentially needed by Kura Wires Visualization model and
     * in case the implementors need to use specific driver implementation in
     * complete isolation, the implementors can return {@code null}.<br/>
     * <br/>
     *
     * Furthermore, some implementors can also provide custom objects. In such
     * case, implementors must also provide another implementation for providing
     * configurable properties of a {@link ConfigurableComponent} using the
     * provided custom object. As currently the {@code AD} uses OSGi
     * Configuration Admin service to provide configurable properties, the
     * custom object class needs to use OSGi Configuration Admin service for
     * that same purpose.
     *
     * @return the protocol specific descriptor
     */
    public Object getDescriptor();

}
