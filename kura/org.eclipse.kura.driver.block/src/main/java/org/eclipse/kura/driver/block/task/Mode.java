/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.driver.block.task;

/**
 * Describes an operation performed by a {@link BlockTask}.
 */
public enum Mode {
    /**
     * The operation is a read
     */
    READ,
    /**
     * The operation is a write
     */
    WRITE,
    /**
     * The operation is a read-update-write.
     *
     * @see UpdateBlockTask
     * @see UpdateBlockTaskAggregator
     */
    UPDATE
}
