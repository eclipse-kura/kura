/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.server.net2;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.web.server.net2.configuration.NetworkConfigurationServiceAdapter;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GwtNetworkServiceImpl {

    private static final Logger logger = LoggerFactory.getLogger(GwtNetworkServiceImpl.class);

    public static List<GwtNetInterfaceConfig> findNetInterfaceConfigurations(boolean recompute)
            throws GwtKuraException {
        try {
            NetworkConfigurationServiceAdapter configuration = new NetworkConfigurationServiceAdapter();

            List<GwtNetInterfaceConfig> result = new LinkedList<>();
            for (String ifname : configuration.getNetInterfaces()) {
                GwtNetInterfaceConfig gwtConfig = configuration.getGwtNetInterfaceConfig(ifname);
                // TODO: use the status service to complete the properties
                // e.g. status.getGwtNetInterfaceConfig
                result.add(gwtConfig);
            }

            return result;
        } catch (KuraException e) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
        }
    }

}
