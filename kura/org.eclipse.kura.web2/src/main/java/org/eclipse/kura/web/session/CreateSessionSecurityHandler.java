/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.session;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.kura.web.Console;

public class CreateSessionSecurityHandler implements SecurityHandler {

    @Override
    public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {

        Console.instance().createSession(request, response);

        return true;
    }

}
