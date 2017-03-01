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
import java.util.*;

/**
 * @author IBM
 * @version 1.2.0
 * @since 1.0
 */
public abstract class ParallelPort extends CommPort {
	/**
	 * Define the lpt mode any (int) constant.
	 */
	public static final int LPT_MODE_ANY = 0;

	/**
	 * Define the lpt mode spp (int) constant.
	 */
	public static final int LPT_MODE_SPP = 1;

	/**
	 * Define the lpt mode ps2 (int) constant.
	 */
	public static final int LPT_MODE_PS2 = 2;

	/**
	 * Define the lpt mode epp (int) constant.
	 */
	public static final int LPT_MODE_EPP = 3;

	/**
	 * Define the lpt mode ecp (int) constant.
	 */
	public static final int LPT_MODE_ECP = 4;

	/**
	 * Define the lpt mode nibble (int) constant.
	 */
	public static final int LPT_MODE_NIBBLE = 5;

	/**
	 * Constructs an instance of this class.
	 */
	protected ParallelPort() {
		super();
	}

	/**
	 * Add event listener with the specified lsnr parameter.
	 * @param lsnr	The lsnr (<code>ParallelPortEventListener</code>) parameter.
	 * @throws TooManyListenersException Too Many Listeners Exception.
	 * @see #removeEventListener()
	 */
	public abstract void addEventListener(final ParallelPortEventListener lsnr) throws TooManyListenersException;

	/**
	 * Gets the mode (int) value.
	 * @return	The mode (<code>int</code>) value.
	 * @see #setMode(int)
	 */
	public abstract int getMode();

	/**
	 * Gets the output buffer free (int) value.
	 * @return	The output buffer free (<code>int</code>) value.
	 */
	public abstract int getOutputBufferFree();

	/**
	 * Gets the paper out (boolean) value.
	 * @return	The paper out (<code>boolean</code>) value.
	 */
	public abstract boolean isPaperOut();

	/**
	 * Gets the printer busy (boolean) value.
	 * @return	The printer busy (<code>boolean</code>) value.
	 */
	public abstract boolean isPrinterBusy();

	/**
	 * Gets the printer error (boolean) value.
	 * @return	The printer error (<code>boolean</code>) value.
	 */
	public abstract boolean isPrinterError();

	/**
	 * Gets the printer selected (boolean) value.
	 * @return	The printer selected (<code>boolean</code>) value.
	 */
	public abstract boolean isPrinterSelected();

	/**
	 * Gets the printer timed out (boolean) value.
	 * @return	The printer timed out (<code>boolean</code>) value.
	 */
	public abstract boolean isPrinterTimedOut();

	/**
	 * Notify on buffer with the specified notify parameter.
	 * @param notify	The notify (<code>boolean</code>) parameter.
	 */
	public abstract void notifyOnBuffer(final boolean notify);

	/**
	 * Notify on error with the specified notify parameter.
	 * @param notify	The notify (<code>boolean</code>) parameter.
	 */
	public abstract void notifyOnError(final boolean notify);

	/**
	 * Remove event listener.
	 * @see #addEventListener(ParallelPortEventListener)
	 */
	public abstract void removeEventListener();

	/**
	 * Restart.
	 */
	public abstract void restart();

	/**
	 * Sets the mode value.
	 * @param mode	The mode (<code>int</code>) parameter.
	 * @return	The mode (<code>int</code>) value.
	 * @throws UnsupportedCommOperationException Unsupported Comm Operation Exception.
	 * @see #getMode()
	 */
	public abstract int setMode(final int mode) throws UnsupportedCommOperationException;

	/**
	 * Suspend.
	 */
	public abstract void suspend();
}
