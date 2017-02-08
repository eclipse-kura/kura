/*******************************************************************************
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.wire;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.annotation.Immutable;
import org.eclipse.kura.annotation.ThreadSafe;
import org.eclipse.kura.type.TypedValue;

/**
 * The Class WireRecord represents a record to be transmitted during wire
 * communication between wire emitter and wire receiver
 *
 * @noextend This class is not intended to be extended by clients.
 */
@Immutable
@ThreadSafe
public class WireRecord {

    private final Map<String, TypedValue<?>> properties;

    /**
     * Instantiates a new {@link WireRecord}.
     *
     * @param properties
     *            Map that represents the key-value pairs
     * @throws NullPointerException
     *             if any of the argument is null
     */
    public WireRecord(Map<String, TypedValue<?>> properties) {
        requireNonNull(properties, "Properties cannot be null");

        this.properties = new HashMap<>(properties);
    }

    /**
     * Returns the properties stored in this {@link WireRecord}
     *
     * @return the fields
     */
    public Map<String, TypedValue<?>> getProperties() {
        return Collections.unmodifiableMap(this.properties);
    }
}
