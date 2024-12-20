/*******************************************************************************
 * Copyright (c) 2019, 2024 Eurotech and/or its affiliates and others
 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.http.server.manager;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;

public class KuraErrorHandler extends ErrorHandler {

    @Override
    protected void writeErrorHtml(Request request, Writer writer, Charset charset, int code, String message,
            Throwable cause, boolean showStacks) throws IOException {
        // do nothing
    }

    @Override
    protected void writeErrorPlain(Request request, PrintWriter writer, int code, String message, Throwable cause,
            boolean showStacks) {
        // do nothing
    }

    @Override
    protected void writeErrorJson(Request request, PrintWriter writer, int code, String message, Throwable cause,
            boolean showStacks) {
        // do nothing
    }

}
