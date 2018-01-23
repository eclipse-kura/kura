/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.wire.multiport;

import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireReceiver;
import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.service.wireadmin.Producer;

/**
 * The MultiportWireEmitter is a marker interface which represents a wire component which
 * is a data producer that can produce values. The produced values can be used
 * by other {@link WireReceiver} components if it is wired with each other.
 * 
 * @since 1.4
 */
@ConsumerType
public interface MultiportWireEmitter extends Producer, WireComponent {

}
