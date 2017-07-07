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
 * This class represents a prohibited interval over an abstract addressing space, that is an interval of addresses on
 * which it is not permitted to perform a specific operation (e.g. a non readable or non writable area).
 *
 * @see Block
 */
public class ProhibitedBlock extends Block {

    public ProhibitedBlock(int start, int end) {
        super(start, end);
    }

}
