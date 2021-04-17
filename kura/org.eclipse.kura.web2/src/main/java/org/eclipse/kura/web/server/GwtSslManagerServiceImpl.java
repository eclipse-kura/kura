/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.ssl.SslManagerService;
import org.eclipse.kura.web.shared.service.GwtSslManagerService;

public class GwtSslManagerServiceImpl extends BaseGwtRestrictedComponentService implements GwtSslManagerService {

    /**
     * 
     */
    private static final long serialVersionUID = -1480613592267828475L;

    public GwtSslManagerServiceImpl() {
        super(i -> i.contains(SslManagerService.class.getName()) && i.contains(ConfigurableComponent.class.getName()));
    }

}
