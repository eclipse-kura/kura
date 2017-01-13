/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.gpio;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * This interface is used to notify status change on the Input pins
 *
 */
@ConsumerType
public interface PinStatusListener {

    /**
     * Invoked when the status of the attached input pin changes
     *
     * @param value
     *            The new value of the pin.
     */
    public void pinStatusChange(boolean value);
}
