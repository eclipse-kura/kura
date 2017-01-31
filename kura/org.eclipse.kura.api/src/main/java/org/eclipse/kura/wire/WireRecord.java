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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.eclipse.kura.annotation.Nullable;
import org.osgi.util.position.Position;

/**
 * The Class WireRecord represents a record to be transmitted during wire
 * communication between wire emitter and wire receiver
 *
 * @noextend This class is not intended to be extended by clients.
 */
public class WireRecord {

    private final HashSet<WireField> fields = new HashSet<>();

    @Nullable
    private final Position position;

    private final Timestamp timestamp;

    /**
     * Instantiates a new wire record.
     *
     * @param timestamp
     *            the timestamp
     * @param fields
     *            the wire fields
     * @throws NullPointerException
     *             if any of the argument is null
     */
    public WireRecord(Timestamp timestamp) {
        requireNonNull(timestamp, "Timestamp cannot be null");

        this.timestamp = timestamp;
        this.position = null;
    }

    /**
     * Instantiates a new wire record.
     *
     * @param timestamp
     *            the timestamp
     * @param position
     *            the position
     * @param fields
     *            the wire fields
     * @throws NullPointerException
     *             if any of the argument is null (except position)
     */
    public WireRecord(final Timestamp timestamp, final Position position) {
        requireNonNull(timestamp, "Timestamp cannot be null");

        this.timestamp = timestamp;
        this.position = position;
    }

    /**
     * Gets the associated fields.
     *
     * @return the fields
     */
    public List<WireField> getFields() {
        return Collections.unmodifiableList(new ArrayList<>(this.fields));
    }

    /**
     * Gets the position.
     *
     * @return the position
     */
    public Position getPosition() {
        return this.position;
    }

    /**
     * Gets the timestamp.
     *
     * @return the timestamp
     */
    public Timestamp getTimestamp() {
        return this.timestamp;
    }

    public void addField(WireField field) {

        boolean addResult = this.fields.add(field);
        if (!addResult) {
            throw new IllegalArgumentException();
        }
    }

}
