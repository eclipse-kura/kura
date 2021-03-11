/*******************************************************************************
 * Copyright (c) 2020, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.server;

import java.util.Set;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.web.Console;
import org.eclipse.kura.web.ConsoleOptions;
import org.eclipse.kura.web.server.util.GwtServerUtil;
import org.eclipse.kura.web.shared.model.GwtLoginInfo;
import org.eclipse.kura.web.shared.service.GwtLoginInfoService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GwtLoginInfoServiceImpl extends OsgiRemoteServiceServlet implements GwtLoginInfoService {

    private static final Logger logger = LoggerFactory.getLogger(GwtLoginInfoServiceImpl.class);

    private static final long serialVersionUID = 1L;

    @Override
    public GwtLoginInfo getLoginInfo() {
        final ConsoleOptions options = Console.getConsoleOptions();

        final String bannerContent;

        if (options.isBannerEnabled()) {
            bannerContent = options.getBannerContent();
        } else {
            bannerContent = null;
        }

        Integer clientAuthPort = null;

        try {

            final BundleContext bundleContext = FrameworkUtil.getBundle(GwtLoginInfoServiceImpl.class)
                    .getBundleContext();

            final ServiceReference<?> ref = bundleContext.getServiceReferences(ConfigurableComponent.class.getName(),
                    "(service.pid=org.eclipse.kura.http.server.manager.HttpService)")[0];

            final Set<Integer> clientAuthPorts = GwtServerUtil
                    .getArrayProperty(ref.getProperty("https.client.auth.ports"), Integer.class);

            for (final Integer port : clientAuthPorts) {
                if (Console.getConsoleOptions().isPortAllowed(port)) {
                    clientAuthPort = port;
                    break;
                }
            }

        } catch (final Exception e) {
            logger.warn("failed to determine HTTP cert auth port", e);
            clientAuthPort = null;
        }

        return new GwtLoginInfo(bannerContent, options.getEnabledAuthMethods(), clientAuthPort);
    }

}
