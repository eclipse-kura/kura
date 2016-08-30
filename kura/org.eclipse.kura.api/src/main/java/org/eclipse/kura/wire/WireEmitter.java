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
package org.eclipse.kura.wire;

import org.osgi.service.wireadmin.Producer;

/**
 * The WireEmitter is a marker interface which represents a wire component which
 * is a data producer that can produce values. The produced values can be used
 * by other {@link WireReceiver} components if it is wired with each other.
 */
public interface WireEmitter extends WireComponent, Producer {
}
