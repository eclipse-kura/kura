/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.core.comm;

import static org.eclipse.kura.comm.CommURI.parseString;

import java.io.IOException;

import javax.microedition.io.Connection;

import org.osgi.service.io.ConnectionFactory;

public class CommConnectionFactory implements ConnectionFactory {

    @Override
    public Connection createConnection(String name, int mode, boolean timeouts) throws IOException {
        try {
            return new CommConnectionImpl(parseString(name), mode, timeouts);
        } catch (IOException e) {
            throw e; // re-throw
        } catch (Throwable t) {
            throw new IOException(t);
        }
    }
}
