/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.bluetooth;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connection;
import javax.microedition.io.ConnectionNotFoundException;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.io.ConnectorService;

/**
 * @noimplement This interface is not intended to be implemented by clients.
 *
 * @deprecated
 *
 */
@ProviderType
@Deprecated
public interface BluetoothConnector extends ConnectorService {

    /**
     * Access mode READ
     */
    public static final int READ = 1;

    /**
     * Access mode WRITE
     */
    public static final int WRITE = 2;

    /**
     * Access mode READ_WRITE
     */
    public static final int READ_WRITE = 3;

    /**
     * Create and open a Connection.
     *
     * @param name
     *            The URL for the connection.
     *
     * @return A new Connection object.
     *
     * @throws IllegalArgumentException
     *             If a parameter is invalid.
     * @throws ConnectionNotFoundException
     *             If the requested connection cannot be made,
     *             or the protocol type does not exist.
     * @throws java.io.IOException
     *             If some other kind of I/O error occurs.
     * @throws SecurityException
     *             If a requested protocol handler is not permitted.
     */
    @Deprecated
    @Override
    public Connection open(String name) throws IOException;

    /**
     * Create and open a Connection.
     *
     * @param name
     *            The URL for the connection.
     * @param mode
     *            The access mode.
     *
     * @return A new Connection object.
     *
     * @throws IllegalArgumentException
     *             If a parameter is invalid.
     * @throws ConnectionNotFoundException
     *             If the requested connection cannot be made,
     *             or the protocol type does not exist.
     * @throws java.io.IOException
     *             If some other kind of I/O error occurs.
     * @throws SecurityException
     *             If a requested protocol handler is not permitted.
     */
    @Deprecated
    @Override
    public Connection open(String name, int mode) throws IOException;

    /**
     * Create and open a Connection.
     *
     * @param name
     *            The URL for the connection.
     * @param mode
     *            The access mode.
     * @param timeouts
     *            A flag to indicate that the caller wants timeout exceptions
     *
     * @return A new Connection object.
     *
     * @throws IllegalArgumentException
     *             If a parameter is invalid.
     * @throws ConnectionNotFoundException
     *             If the requested connection cannot be made,
     *             or the protocol type does not exist.
     * @throws java.io.IOException
     *             If some other kind of I/O error occurs.
     * @throws SecurityException
     *             If a requested protocol handler is not permitted.
     */
    @Deprecated
    @Override
    public Connection open(String name, int mode, boolean timeouts) throws IOException;

    /**
     * Create and open a connection input stream.
     *
     * @param name
     *            The URL for the connection.
     * @return A DataInputStream.
     *
     * @throws IllegalArgumentException
     *             If a parameter is invalid.
     * @throws ConnectionNotFoundException
     *             If the connection cannot be found.
     * @throws java.io.IOException
     *             If some other kind of I/O error occurs.
     * @throws SecurityException
     *             If access to the requested stream is not permitted.
     */
    @Deprecated
    @Override
    public DataInputStream openDataInputStream(String name) throws IOException;

    /**
     * Create and open a connection output stream.
     *
     * @param name
     *            The URL for the connection.
     * @return A DataOutputStream.
     *
     * @throws IllegalArgumentException
     *             If a parameter is invalid.
     * @throws ConnectionNotFoundException
     *             If the connection cannot be found.
     * @throws java.io.IOException
     *             If some other kind of I/O error occurs.
     * @throws SecurityException
     *             If access to the requested stream is not permitted.
     */
    @Deprecated
    @Override
    public DataOutputStream openDataOutputStream(String name) throws IOException;

    /**
     * Create and open a connection input stream.
     *
     * @param name
     *            The URL for the connection.
     * @return An InputStream
     *
     * @throws IllegalArgumentException
     *             If a parameter is invalid.
     * @throws ConnectionNotFoundException
     *             If the connection cannot be found.
     * @throws java.io.IOException
     *             If some other kind of I/O error occurs.
     * @throws SecurityException
     *             If access to the requested stream is not permitted.
     */
    @Deprecated
    @Override
    public InputStream openInputStream(String name) throws IOException;

    /**
     * Create and open a connection output stream.
     *
     * @param name
     *            The URL for the connection.
     * @return An OutputStream
     *
     * @throws IllegalArgumentException
     *             If a parameter is invalid.
     * @throws ConnectionNotFoundException
     *             If the connection cannot be found.
     * @throws java.io.IOException
     *             If some other kind of I/O error occurs.
     * @throws SecurityException
     *             If access to the requested stream is not permitted.
     */
    @Deprecated
    @Override
    public OutputStream openOutputStream(String name) throws IOException;

}
