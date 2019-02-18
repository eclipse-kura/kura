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
