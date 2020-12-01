/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.wire.multiport;

import org.eclipse.kura.wire.WireComponent;
import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.service.wireadmin.Producer;

/**
 * The MultiportWireEmitter is a marker interface which represents a wire component which
 * is a data producer that can produce values. The produced values can be used
 * by other {@link org.eclipse.kura.wire.WireReceiver} components if it is wired with each other.
 *
 * @since 1.4
 */
@ConsumerType
public interface MultiportWireEmitter extends Producer, WireComponent {

}
