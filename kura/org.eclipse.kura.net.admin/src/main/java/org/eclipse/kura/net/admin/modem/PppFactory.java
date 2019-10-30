/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.admin.modem;

import org.eclipse.kura.executor.CommandExecutorService;

public final class PppFactory {

    private PppFactory() {
    }

    public static IModemLinkService getPppService(final String interfaceName, final String port,
            final CommandExecutorService executorService) {
        return new Ppp(interfaceName, port, executorService);
    }
}
