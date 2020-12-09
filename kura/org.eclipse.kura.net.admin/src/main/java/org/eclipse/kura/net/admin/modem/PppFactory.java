/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
