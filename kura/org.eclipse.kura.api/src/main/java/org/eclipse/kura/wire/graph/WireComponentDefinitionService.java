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

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * This WireComponentDefinitionService allows to fetch the {@link WireComponentDefinition}s of the registered Wire
 * Components.
 *
 * @since 1.4
 */
@ProviderType
public interface WireComponentDefinitionService {

    /**
     * This method allows to list the {@link WireComponentDefinition}s for the registered Wire Components.
     *
     * @return a list of registered {@link WireComponentDefinition}s
     * @throws KuraException
     *             if the get operation fails.
     */
    public List<WireComponentDefinition> getComponentDefinitions() throws KuraException;

}
