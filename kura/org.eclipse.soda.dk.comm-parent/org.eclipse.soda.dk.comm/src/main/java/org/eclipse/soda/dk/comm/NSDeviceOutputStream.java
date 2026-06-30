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
import java.io.OutputStream;

import javax.comm.CommPort;
import javax.comm.CommPortIdentifier;
import javax.comm.ParallelPortEvent;
import javax.comm.SerialPortEvent;

/**
 * @author IBM
 */
public class NSDeviceOutputStream extends OutputStream {
// -----------------------------------------------------------------------------
// Variables
// -----------------------------------------------------------------------------
	int fd = -1;

	private int pt; // determines whether to use sp or pp; 1 = p, 2 = s

	private int bufsize;

	private NSSerialPort sp = null;

	private NSParallelPort pp = null;

	private byte[] buffer = null; // size equivalent to insBufferSize

	// of NSSerialPort or NSParallelPort
// -----------------------------------------------------------------------------
// Methods - constructors
// -----------------------------------------------------------------------------
	NSDeviceOutputStream(final CommPort port, final int portType) {
		if (portType == CommPortIdentifier.PORT_PARALLEL) {
			this.pt = 1;
			this.pp = (NSParallelPort) port;
			this.bufsize = this.pp.outsBufferSize;
		} else {
			this.pt = 2;
			this.sp = (NSSerialPort) port;
			this.bufsize = this.sp.outsBufferSize;
		}
		this.buffer = new byte[this.bufsize];
	}

// -----------------------------------------------------------------------------
// Methods - public
// -----------------------------------------------------------------------------
	public void flush() throws IOException {
		int rc;
		int obc = 0;
		if (this.pp != null) {
			obc = this.pp.outsBufferCount;
		} else if (this.sp != null) {
			obc = this.sp.outsBufferCount;
		}
		rc = writeDeviceNC(this.buffer, 0, obc);
		// If any errors were encountered during writes to the device, throw
		// an exception.
		if (rc != obc) {
			final IOException e = new IOException();
			throw e;
		}
	}

	/**
	 * @return pt
	 */
	public int getPt() {
		return this.pt;
	}

	public void write(final byte b[]) throws IOException {
		write(b, 0, b.length);
	}

	public void write(final byte b[], final int off, final int len) throws IOException {
		// ???? How about if data is to be suspended for the parallel port ????
		int toff = off;
		int tlen = len;
		int rc;
		boolean excflag = false;
		int obc = 0;
		final int oldbufsize = this.bufsize;
		boolean notify = false;
		this.bufsize = 0;
		// If buffer size has been changed since the last time, flush out the
		// current data in the internal buffer.
		if (this.pp != null) {
			this.bufsize = this.pp.outsBufferSize;
			obc = this.pp.outsBufferCount;
		} else if (this.sp != null) {
			this.bufsize = this.sp.outsBufferSize;
			obc = this.sp.outsBufferCount;
		}
		if (this.bufsize != oldbufsize) {
			if (obc != 0) {
				rc = writeDeviceNC(this.buffer, 0, obc);
				if (this.pp != null) {
					this.pp.outsBufferCount = 0;
				} else if (this.sp != null) {
					this.sp.outsBufferCount = 0;
				}
				if (rc != obc) {
					excflag = true;
				}
			}
			this.buffer = null; // hopefully this is freed now
			this.buffer = new byte[this.bufsize];
		}
		// If buffer size is 0, then no buffering is to be done.
		if (this.bufsize == 0) {
			if (this.pp != null) {
				this.pp.outsBufferCount = 0;
			} else if (this.sp != null) {
				this.sp.outsBufferCount = 0;
			}
			rc = writeDeviceNC(b, toff, tlen);
			if (rc != tlen) {
				excflag = true;
			}
		} else { // save the data internally, and if full, write it out
			while (tlen != 0) {
				int wc;
				if (this.pp != null) {
					obc = this.pp.outsBufferCount;
				} else if (this.sp != null) {
					obc = this.sp.outsBufferCount;
				}
				if (obc + tlen >= this.bufsize) {
					wc = this.bufsize - obc;
					// save this in buffer and write buffer to dev
					java.lang.System.arraycopy(b, toff, this.buffer, obc, wc);
					rc = writeDeviceNC(this.buffer, 0, this.bufsize);
					if (this.pp != null) {
						this.pp.outsBufferCount = 0;
					} else if (this.sp != null) {
						this.sp.outsBufferCount = 0;
					}
					if (rc != this.bufsize) {
						excflag = true;
						break;
					}
				} else {
					wc = tlen;
					// save this in buffer
					java.lang.System.arraycopy(b, toff, this.buffer, obc, wc);
					if (this.pp != null) {
						this.pp.outsBufferCount += wc;
					} else if (this.sp != null) {
						this.sp.outsBufferCount += wc;
					}
				}
				toff += wc;
				tlen -= wc;
			}
		}
		// If any errors were encountered during writes to the device, throw
		// an exception.
		if (excflag) {
			final IOException e = new IOException();
			throw e;
		}
		// If the internal buffer has been drained out to the device, send
		// a corresponding event, if notification is set.
		if (this.pp != null) {
			obc = this.pp.outsBufferCount;
			notify = this.pp.notifyOnBufferFlag;
		} else if (this.sp != null) {
			obc = this.sp.outsBufferCount;
			notify = this.sp.notifyOnBufferFlag;
		}
		if (notify && (obc == 0)) {
			if (this.pp != null) {
				this.pp.reportParallelEvent(ParallelPortEvent.PAR_EV_BUFFER, false, true);
			} else if (this.sp != null) {
				this.sp.reportSerialEvent(SerialPortEvent.OUTPUT_BUFFER_EMPTY, false, true);
			}
		}
	}

	public void write(final int i) throws IOException {
		final byte b[] = new byte[1];
		b[0] = (byte) (i & 0xff);
		write(b, 0, 1);
	}

// -----------------------------------------------------------------------------
// Methods - private
// -----------------------------------------------------------------------------
	private native int writeDeviceNC(byte buf[], int offset, int nBytes);
}
