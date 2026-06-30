package javax.comm;

/*************************************************************************
 * Copyright (c) 1999, 2009 IBM.                                         *
 * All rights reserved. This program and the accompanying materials      *
 * are made available under the terms of the Eclipse Public License v1.0 *
 * which accompanies this distribution, and is available at              *
 * http://www.eclipse.org/legal/epl-v10.html                             *
 *                                                                       *
 * Contributors:                                                         *
 *     IBM - initial API and implementation                              *
 ************************************************************************/
import java.io.*;

/**
 * @author IBM
 * @version 1.2.0
 * @since 1.0
 */
public abstract class CommPort extends Object {
	/**
	 * Define the name (String) field.
	 */
	protected String name = null;

	/**
	 * Close.
	 */
	public void close() {
		CommPortIdentifier cport;
		try {
			cport = CommPortIdentifier.getPortIdentifier(this);
		} catch (final NoSuchPortException nspe) {
			cport = null;
		}
		if (cport != null) {
			cport.internalClosePort();
		}
	}

	/**
	 * Disable receive framing.
	 * @see #enableReceiveFraming(int)
	 */
	public abstract void disableReceiveFraming();

	/**
	 * Disable receive threshold.
	 * @see #enableReceiveThreshold(int)
	 * @see #getReceiveThreshold()
	 */
	public abstract void disableReceiveThreshold();

	/**
	 * Disable receive timeout.
	 * @see #enableReceiveTimeout(int)
	 * @see #getReceiveTimeout()
	 */
	public abstract void disableReceiveTimeout();

	/**
	 * Enable receive framing with the specified framing byte parameter.
	 * @param framingByte	The framing byte (<code>int</code>) parameter.
	 * @throws UnsupportedCommOperationException Unsupported Comm Operation Exception.
	 * @see #disableReceiveFraming()
	 */
	public abstract void enableReceiveFraming(final int framingByte) throws UnsupportedCommOperationException;

	/**
	 * Enable receive threshold with the specified thresh parameter.
	 * @param thresh	The thresh (<code>int</code>) parameter.
	 * @throws UnsupportedCommOperationException Unsupported Comm Operation Exception.
	 * @see #disableReceiveThreshold()
	 * @see #getReceiveThreshold()
	 */
	public abstract void enableReceiveThreshold(final int thresh) throws UnsupportedCommOperationException;

	/**
	 * Enable receive timeout with the specified rcv timeout parameter.
	 * @param rcvTimeout	The rcv timeout (<code>int</code>) parameter.
	 * @throws UnsupportedCommOperationException Unsupported Comm Operation Exception.
	 * @see #disableReceiveTimeout()
	 * @see #getReceiveTimeout()
	 */
	public abstract void enableReceiveTimeout(final int rcvTimeout) throws UnsupportedCommOperationException;

	/**
	 * Gets the input buffer size (int) value.
	 * @return	The input buffer size (<code>int</code>) value.
	 * @see #setInputBufferSize(int)
	 */
	public abstract int getInputBufferSize();

	/**
	 * Gets the input stream value.
	 * @return	The input stream (<code>InputStream</code>) value.
	 * @throws IOException IOException.
	 */
	public abstract InputStream getInputStream() throws IOException;

	/**
	 * Gets the name (String) value.
	 * @return	The name (<code>String</code>) value.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Gets the output buffer size (int) value.
	 * @return	The output buffer size (<code>int</code>) value.
	 * @see #setOutputBufferSize(int)
	 */
	public abstract int getOutputBufferSize();

	/**
	 * Gets the output stream value.
	 * @return	The output stream (<code>OutputStream</code>) value.
	 * @throws IOException IOException.
	 */
	public abstract OutputStream getOutputStream() throws IOException;

	/**
	 * Gets the receive framing byte (int) value.
	 * @return	The receive framing byte (<code>int</code>) value.
	 */
	public abstract int getReceiveFramingByte();

	/**
	 * Gets the receive threshold (int) value.
	 * @return	The receive threshold (<code>int</code>) value.
	 * @see #disableReceiveThreshold()
	 * @see #enableReceiveThreshold(int)
	 */
	public abstract int getReceiveThreshold();

	/**
	 * Gets the receive timeout (int) value.
	 * @return	The receive timeout (<code>int</code>) value.
	 * @see #disableReceiveTimeout()
	 * @see #enableReceiveTimeout(int)
	 */
	public abstract int getReceiveTimeout();

	/**
	 * Gets the receive framing enabled (boolean) value.
	 * @return	The receive framing enabled (<code>boolean</code>) value.
	 */
	public abstract boolean isReceiveFramingEnabled();

	/**
	 * Gets the receive threshold enabled (boolean) value.
	 * @return	The receive threshold enabled (<code>boolean</code>) value.
	 */
	public abstract boolean isReceiveThresholdEnabled();

	/**
	 * Gets the receive timeout enabled (boolean) value.
	 * @return	The receive timeout enabled (<code>boolean</code>) value.
	 */
	public abstract boolean isReceiveTimeoutEnabled();

	/**
	 * Sets the input buffer size value.
	 * @param size	The size (<code>int</code>) parameter.
	 * @see #getInputBufferSize()
	 */
	public abstract void setInputBufferSize(final int size);

	/**
	 * Sets the output buffer size value.
	 * @param size	The size (<code>int</code>) parameter.
	 * @see #getOutputBufferSize()
	 */
	public abstract void setOutputBufferSize(final int size);

	/**
	 * Returns the string value.
	 * @return	The string (<code>String</code>) value.
	 */
	public String toString() {
		String str;
		String pt;
		CommPortIdentifier cport;
		/* This should be port name + port type. */
		try {
			cport = CommPortIdentifier.getPortIdentifier(this);
		} catch (final NoSuchPortException nspe) {
			cport = null;
		}
		if (cport != null) {
			pt = cport.getPortType() == CommPortIdentifier.PORT_SERIAL ? "SERIAL" : "PARALLEL"; //$NON-NLS-1$//$NON-NLS-2$
		} else {
			pt = " "; //$NON-NLS-1$
		}
		str = new String(this.name + ':' + pt);
		return str;
	}
}
