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
