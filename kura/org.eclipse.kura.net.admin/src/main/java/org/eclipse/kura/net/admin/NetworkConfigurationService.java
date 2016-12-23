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
package org.eclipse.kura.net.admin;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.NetworkConfiguration;

public interface NetworkConfigurationService {

    public static final String PID = "org.eclipse.kura.net.admin.NetworkConfigurationService";

    public void setNetworkConfiguration(NetworkConfiguration networkConfiguration) throws KuraException;

    public NetworkConfiguration getNetworkConfiguration() throws KuraException;

}
