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

import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The GPIOService is used to access available GPIO resources on the system.<br>
 * {@link KuraGPIOPin}s can be accessed by name or by terminal index.<br>
 * <br>
 * Operations on the pins can be done using the acquired {@link KuraGPIOPin} class.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface GPIOService {

    public KuraGPIOPin getPinByName(String pinName);

    public KuraGPIOPin getPinByName(String pinName, KuraGPIODirection direction, KuraGPIOMode mode,
            KuraGPIOTrigger trigger);

    public KuraGPIOPin getPinByTerminal(int terminal);

    public KuraGPIOPin getPinByTerminal(int terminal, KuraGPIODirection direction, KuraGPIOMode mode,
            KuraGPIOTrigger trigger);

    public Map<Integer, String> getAvailablePins();

}
