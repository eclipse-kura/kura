/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *
 *******************************************************************************/
package org.eclipse.kura.wire.graph;

import java.util.List;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.wireadmin.Wire;

/**
 * This interface represents the port(s) associated to a Wire Component.
 * 
 * @since 1.4
 */
@ProviderType
public interface Port {

    /**
     * This method returns the list of {@link Wire}s connected to this {@link Port}
     * 
     * @return the list of {@link Wire} connected to this {@link Port}
     */
    public List<Wire> listConnectedWires();

}
