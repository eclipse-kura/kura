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

import static java.util.Objects.requireNonNull;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.eclipse.kura.annotation.Immutable;
import org.eclipse.kura.annotation.Nullable;
import org.eclipse.kura.annotation.ThreadSafe;
import org.osgi.util.position.Position;

/**
 * The Class WireRecord represents a record to be transmitted during wire
 * communication between wire emitter and wire receiver
 */
@Immutable
@ThreadSafe
public final class WireRecord {

    /** The contained wire fields. */
    private final List<WireField> fields;

    /** The position. */
    @Nullable
    private final Position position;

    /** The timestamp. */
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
    public WireRecord(final Timestamp timestamp, final List<WireField> fields) {
        requireNonNull(timestamp, "Timestamp cannot be null");
        requireNonNull(fields, "Wire fields cannot be null");

        this.timestamp = timestamp;
        this.position = null;
        this.fields = Collections.unmodifiableList(fields);
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
    public WireRecord(final Timestamp timestamp, @Nullable final Position position, final List<WireField> fields) {
        requireNonNull(timestamp, "Timestamp cannot be null");
        requireNonNull(fields, "Wire fields cannot be null");

        this.timestamp = timestamp;
        this.position = position;
        this.fields = Collections.unmodifiableList(fields);
    }

    /**
     * Instantiates a new wire record.
     *
     * @param fields
     *            the wire fields
     * @throws NullPointerException
     *             if any of the argument is null
     */
    public WireRecord(final WireField... fields) {
        requireNonNull(fields, "Wire fields cannot be null");
        this.timestamp = new Timestamp(new Date().getTime());
        this.position = null;
        this.fields = Collections.unmodifiableList(Arrays.asList(fields));
    }

    /**
     * Gets the associated fields.
     *
     * @return the fields
     */
    public List<WireField> getFields() {
        return this.fields;
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

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "WireRecord [fields=" + this.fields + ", position=" + this.position + ", timestamp=" + this.timestamp
                + "]";
    }

}
