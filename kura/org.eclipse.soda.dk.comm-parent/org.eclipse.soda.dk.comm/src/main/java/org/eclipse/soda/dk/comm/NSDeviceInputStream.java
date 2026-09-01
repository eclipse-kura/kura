package org.eclipse.soda.dk.comm;

/*******************************************************************************
 * Copyright (c) 1999, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
import java.io.IOException;
import java.io.InputStream;

import javax.comm.CommPort;
import javax.comm.CommPortIdentifier;

/**
 * @author IBM
 */
public class NSDeviceInputStream extends InputStream {
// -----------------------------------------------------------------------------
// Variables
// -----------------------------------------------------------------------------
	int fd = -1;

	private int bufsize = 0;

	private int readCount = 0; // read by the app

	private int bufferCount = 0; // read by this object from the device

	private int thc = -1; // threshold value from serial/parallel ports

	private int tmo = -1; // timeout value from serial/parallel ports

	private boolean tmoDone = false; // timeout occurred?

	private NSSerialPort sp = null;

	private NSParallelPort pp = null;

	private byte[] buffer = null; // size equivalent to insBufferSize

	// of NSSerialPort or NSParallelPort
// -----------------------------------------------------------------------------
// Methods - constructors
// -----------------------------------------------------------------------------
	NSDeviceInputStream(final CommPort port, final int portType) {
		if (portType == CommPortIdentifier.PORT_PARALLEL) {
			this.pp = (NSParallelPort) port;
			this.bufsize = this.pp.insBufferSize;
			this.thc = this.pp.rcvThreshold;
		} else {
			this.sp = (NSSerialPort) port;
			this.bufsize = this.sp.insBufferSize;
			this.thc = this.sp.rcvThreshold;
		}
		this.buffer = new byte[this.bufsize];
	}

// -----------------------------------------------------------------------------
// Methods - public
// -----------------------------------------------------------------------------
	public int available() throws IOException {
		int rc;
		int nbc = 0; // no of bytes that can be read without blocking
		// Check to see if any data is in our internal buffer first.
		if (this.bufferCount > this.readCount) {
			nbc += this.bufferCount - this.readCount;
		}
		// Now query the device to see if any and how much data is pending
		// to be read.
		rc = getReadCountNC();
		if (rc > 0) {
			nbc += rc;
		}
		return nbc;
	}

	private native int getReadCountNC() throws IOException;

	public int read() throws IOException {
		int rc;
		// If there is data already read from the device and buffered up,
		// pick up one byte from it. If not, read it from the device.
		if (this.bufferCount > this.readCount) {
			rc = this.buffer[this.readCount++];
			if (this.readCount == this.bufferCount) {
				this.readCount = this.bufferCount = 0;
			}
		} else {
			// Obtain the timeout trigger value.
			if (this.pp != null) {
				this.tmo = this.pp.rcvTimeout;
			} else if (this.sp != null) {
				this.tmo = this.sp.rcvTimeout;
			}
			rc = readDeviceOneByteNC(); // throws IOException
		}
		return rc;
	}

	public int read(final byte b[]) throws IOException {
		return read(b, 0, b.length); // throws IOException
	}

// -----------------------------------------------------------------------------
// Methods - private
// -----------------------------------------------------------------------------
	public int read(final byte b[], final int off, final int len) throws IOException {
		int toff = off;
		int tlen = len;
		int rc;
		boolean excflag = false;
		int rdc = 0;
		// Determine the minimum of length and threshold (if set).
		if (this.pp != null) {
			this.thc = this.pp.rcvThreshold;
			this.tmo = this.pp.rcvTimeout;
		} else if (this.sp != null) {
			this.thc = this.sp.rcvThreshold;
			this.tmo = this.sp.rcvTimeout;
		}
		if ((this.thc > 0) && (this.thc < tlen)) {
			tlen = this.thc;
		}
		// If buffer size is 0, then no buffering is to be done.
		if (this.bufsize == 0) {
			this.readCount = this.bufferCount = 0;
			while (tlen != 0) {
				this.tmoDone = false;
				rc = readDeviceNC(b, toff, tlen);
				if (rc < 0) {
					excflag = true;
					break;
				}
				toff += rc;
				tlen -= rc;
				rdc += rc;
				// If no data had been received this time, discontinue.
				if (rc == 0) {
					break;
				}
				// If threshold is never set, we're done with whatever
				// we got so far.
				if (this.thc < 0) {
					break;
				}
				// If timeout is enabled and timeout occurred,
				// discontinue.
				if ((this.tmo > 0) && this.tmoDone) {
					break;
				}
			} // end of while
		} else {
			while (tlen != 0) {
				int cc;
				this.tmoDone = false;
				// if no data in the buffer, get some data.
				if (this.bufferCount == 0) {
					this.readCount = 0;
					rc = readDeviceNC(this.buffer, 0, this.bufsize);
					if (rc < 0) {
						excflag = true;
						break;
					}
					this.bufferCount = rc;
					// If no data had been received this time,
					// discontinue.
					if (rc == 0) {
						break;
					}
				}
				// Copy the buffered up data.
				cc = tlen <= this.bufferCount - this.readCount ? tlen : this.bufferCount - this.readCount;
				System.arraycopy(this.buffer, this.readCount, b, toff, cc);
				toff += cc;
				tlen -= cc;
				this.readCount += cc;
				rdc += cc;
				if (this.readCount == this.bufferCount) {
					this.readCount = this.bufferCount = 0;
				}
				// If threshold is never set, we're done with whatever
				// we got so far.
				if (this.thc < 0) {
					break;
				}
				// If timeout is enabled and timeout occurred,
				// discontinue.
				if ((this.tmo > 0) && this.tmoDone) {
					break;
				}
			} // end of while
		}
		if (excflag) {
			final IOException e = new IOException();
			throw e;
		}
		return rdc;
	}

	private native int readDeviceNC(byte buf[], int offset, int nBytes);

	// private native int setFDNC();
	private native int readDeviceOneByteNC() throws IOException;
}
