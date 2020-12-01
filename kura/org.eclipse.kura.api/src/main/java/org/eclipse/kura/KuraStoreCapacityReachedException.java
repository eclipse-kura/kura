/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura;

import org.osgi.annotation.versioning.ProviderType;

/**
 * KuraStoreCapacityReachedException is raised when a message can not be appended
 * to the publishing queue as the internal database buffer has reached its
 * capacity for messages that are not yet published or they are still in transit.
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class KuraStoreCapacityReachedException extends KuraStoreException {

    private static final long serialVersionUID = 2622483579047285733L;

    public KuraStoreCapacityReachedException(Object argument) {
        super(argument);
    }
}
