/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/
package org.eclipse.kura.wire;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.wireadmin.Producer;

/**
 * The WireEmitter is a marker interface which represents a wire component which
 * is a data producer that can produce values. The produced values can be used
 * by other {@link WireReceiver} components if it is wired with each other.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface WireEmitter extends WireComponent, Producer {
}
