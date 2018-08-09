/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.board;

import java.io.IOException;

/**
 * Interface to manage board resources, allowing to enable or disable the resource specified by an identifier.
 * The API allows also to reset directly the referred resource and to get the current state.
 *
 */
public interface BoardResourcePowerService {

    /**
     * Enables the resource specified by the passed identifier
     *
     * @param identifier
     *            a String object used to univocally identify a resource in the board.
     * @throws IOException
     */
    public void enable(String identifier) throws IOException;

    /**
     * Disables the resource specified by the passed identifier
     * 
     * @param identifier
     *            a String object used to univocally identify a resource in the board.
     * @throws IOException
     */
    public void disable(String identifier) throws IOException;

    /**
     * Resets the resource specified by the passed identifier
     * 
     * @param identifier
     *            a String object used to univocally identify a resource in the board.
     * @throws IOException
     */
    public void reset(String identifier) throws IOException;

    /**
     * Returns the state of the resource specified by the passed identifier
     * 
     * @param identifier
     *            a String object used to univocally identify a resource in the board.
     * @return a {@link BoardPowerState} representing the current status
     * @throws IOException
     */
    public BoardPowerState getState(String identifier) throws IOException;
}
