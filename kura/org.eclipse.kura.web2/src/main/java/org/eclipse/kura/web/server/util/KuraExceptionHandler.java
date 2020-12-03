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
package org.eclipse.kura.web.server.util;

import org.eclipse.kura.web.shared.GwtKuraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KuraExceptionHandler {

    private static final Logger s_logger = LoggerFactory.getLogger(KuraExceptionHandler.class);

    public static void handle(Throwable t) throws GwtKuraException {
        t.printStackTrace();

        // all others => log and throw internal error code
        s_logger.warn("RPC service non-application error", t);
        throw GwtKuraException.internalError(t, t.getLocalizedMessage());
    }
}
