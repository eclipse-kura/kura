/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.jetty.customizer;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.server.handler.ErrorHandler;

public class KuraErrorHandler extends ErrorHandler {

    @Override
    protected void writeErrorPage(HttpServletRequest request, Writer writer, int code, String message,
            boolean showStacks) throws IOException {
        // Not needed
    }
}
