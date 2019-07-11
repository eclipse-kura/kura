/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
