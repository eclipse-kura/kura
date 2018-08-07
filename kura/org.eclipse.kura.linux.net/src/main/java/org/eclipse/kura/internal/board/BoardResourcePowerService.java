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
 * 
 *
 */
public interface BoardResourcePowerService {

    public void enable(String identifier) throws IOException;

    public void disable(String identifier) throws IOException;

    public void reset(String identifier) throws IOException;

    public BoardPowerState getState(String identifier) throws IOException;
}
