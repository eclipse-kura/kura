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
 * This class is responsible of creating new blocks of type {@code T}.
 *
 * @param <T>
 *            The type of the returned block
 */
public interface BlockFactory<T extends Block> {

    /**
     * Creates a new block of type T, with the given start and end addresses.
     *
     * @param start
     *            the start address of the new block
     * @param end
     *            the end address of the new block
     * @return The new block
     */
    public T build(int start, int end);
}
