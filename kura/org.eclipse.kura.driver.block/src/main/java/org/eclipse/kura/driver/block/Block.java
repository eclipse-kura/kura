/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/

package org.eclipse.kura.driver.block;

/**
 * A {@link Block} instance represents an interval on an abstract addressing space (e.g. a set of consecutive Modbus
 * registers or coils).
 */
public class Block {

    private int start;
    private int end;

    /**
     * Creates a new {@link Block} instance. If the provided start address is lesser than the provided end address, the
     * two addresses will be swapped.
     *
     * @param start
     *            the start address
     * @param end
     *            the end address
     */
    public Block(int start, int end) {
        this.start = Math.min(start, end);
        this.end = Math.max(start, end);
    }

    /**
     * Sets the start address
     *
     * @param start
     *            the start address
     * @throws IllegalArgumentException
     *             If the provided address is greater than the current end address
     */
    public void setStart(int start) {
        if (start > getEnd()) {
            throw new IllegalArgumentException("Start address must be less or equal than end address");
        }
        this.start = start;
    }

    /**
     * Sets the end address
     *
     * @param end
     *            the end address
     * @throws IllegalArgumentException
     *             If the provided address is greater than the current start address
     */
    public void setEnd(int end) {
        if (end < getStart()) {
            throw new IllegalArgumentException("Start address must be less or equal than end address");
        }
        this.end = end;
    }

    /**
     * Returns the start address
     *
     * @return the start address
     */
    public int getStart() {
        return this.start;
    }

    /**
     * Returns the end address
     *
     * @return the end address
     */
    public int getEnd() {
        return this.end;
    }

    /**
     * Checks whether the provided address is contained by the interval represented by this {@link Block}.
     * This method returns true if {@code getStart() <= address && getEnd() > address}
     *
     * @param address
     *            the address to be checked
     * @return {@code true} if the provided address is contained in this {@link Block}, {@code false} otherwise
     */
    public boolean contains(int address) {
        return this.start <= address && this.end > address;
    }

    /**
     * Checks whether the provided {@link Block} is contained by the interval represented by this {@link Block}.
     * This method returns true if {@code this.getStart() <= other.getStart() && this.getEnd() >= other.getEnd()}
     *
     * @param address
     *            the {@link Block} to be checked
     * @return {@code true} if the provided address is contained in this {@link Block}, {@code false} otherwise
     */
    public boolean contains(Block other) {
        return this.start <= other.start && this.end >= other.end;
    }

    @Override
    public String toString() {
        return "[" + this.start + ", " + this.end + "]";
    }
}