/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.server;

import org.eclipse.kura.web.Console;
import org.eclipse.kura.web.ConsoleOptions;
import org.eclipse.kura.web.shared.model.GwtLoginInfo;
import org.eclipse.kura.web.shared.service.GwtLoginInfoService;

public class GwtLoginInfoServiceImpl extends OsgiRemoteServiceServlet implements GwtLoginInfoService {

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

        return new GwtLoginInfo(bannerContent, options.getEnabledAuthMethods());
    }

}
