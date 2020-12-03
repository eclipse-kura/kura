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
 *  Red Hat Inc
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
