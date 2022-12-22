/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.net2;

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The NetworkProviderService provides methods for applying a
 * network configuration to the system.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface NetworkProviderService {

    /**
     * The applyConfiguration method applies the configuration contained in the
     * given {@link org.eclipse.kura.net2.NetworkConfiguration} to the system.
     * 
     * @param configuration The network configuration to be applied to the system
     * @throws KuraException
     */
    public void applyConfiguration(NetworkConfiguration configuration) throws KuraException;
}
