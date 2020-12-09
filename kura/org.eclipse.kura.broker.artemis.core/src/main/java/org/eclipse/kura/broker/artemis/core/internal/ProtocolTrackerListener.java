/*******************************************************************************
 * Copyright (c) 2017, 2020 Red Hat Inc and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.broker.artemis.core.internal;

import java.util.Set;

public interface ProtocolTrackerListener {

    public void protocolsAdded(Set<String> protocols);

    public void protocolsRemoved(Set<String> protocols);
}
