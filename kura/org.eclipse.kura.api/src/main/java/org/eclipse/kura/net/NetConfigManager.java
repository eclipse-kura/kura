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
package org.eclipse.kura.net;

import java.util.Properties;

import org.eclipse.kura.KuraException;

/**
 * @since 2.0
 */
public interface NetConfigManager {

    public void readConfig(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netConfig,
            Properties kuraExtendedProps) throws KuraException;

    public void writeConfig(NetInterfaceConfig<? extends NetInterfaceAddressConfig> netConfig) throws KuraException;

}
