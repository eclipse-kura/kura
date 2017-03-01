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
public abstract class SerialPort extends CommPort {
	/**
	 * Define the databits5 (int) constant.
	 */
	public static final int DATABITS_5 = 5;

	/**
	 * Define the databits6 (int) constant.
	 */
	public static final int DATABITS_6 = 6;

	/**
	 * Define the databits7 (int) constant.
	 */
	public static final int DATABITS_7 = 7;

	/**
	 * Define the databits8 (int) constant.
	 */
	public static final int DATABITS_8 = 8;

	/**
	 * Define the stopbits1 (int) constant.
	 */
	public static final int STOPBITS_1 = 1;

	/**
	 * Define the stopbits2 (int) constant.
	 */
	public static final int STOPBITS_2 = 2;

	/**
	 * Define the stopbits15 (int) constant.
	 */
	public static final int STOPBITS_1_5 = 3;

	/**
	 * Define the parity none (int) constant.
	 */
	public static final int PARITY_NONE = 0;

	/**
	 * Define the parity odd (int) constant.
	 */
	public static final int PARITY_ODD = 1;

	/**
	 * Define the parity even (int) constant.
	 */
	public static final int PARITY_EVEN = 2;

	/**
	 * Define the parity mark (int) constant.
	 */
	public static final int PARITY_MARK = 3;

	/**
	 * Define the parity space (int) constant.
	 */
	public static final int PARITY_SPACE = 4;

	/**
	 * Define the flowcontrol none (int) constant.
	 */
	public static final int FLOWCONTROL_NONE = 0;

	/**
	 * Define the flowcontrol rtscts in (int) constant.
	 */
	public static final int FLOWCONTROL_RTSCTS_IN = 1;

	/**
	 * Define the flowcontrol rtscts out (int) constant.
	 */
	public static final int FLOWCONTROL_RTSCTS_OUT = 2;

	/**
	 * Define the flowcontrol xonxoff in (int) constant.
	 */
	public static final int FLOWCONTROL_XONXOFF_IN = 4;

	/**
	 * Define the flowcontrol xonxoff out (int) constant.
	 */
	public static final int FLOWCONTROL_XONXOFF_OUT = 8;

	/**
	 * Constructs an instance of this class.
	 */
	protected SerialPort() {
		super();
	}

	/**
	 * Add event listener with the specified lsnr parameter.
	 * @param lsnr	The lsnr (<code>SerialPortEventListener</code>) parameter.
	 * @throws TooManyListenersException Too Many Listeners Exception.
	 * @see #removeEventListener()
	 */
	public abstract void addEventListener(final SerialPortEventListener lsnr) throws TooManyListenersException;

	/**
	 * Gets the baud rate (int) value.
	 * @return	The baud rate (<code>int</code>) value.
	 */
	public abstract int getBaudRate();

	/**
	 * Gets the data bits (int) value.
	 * @return	The data bits (<code>int</code>) value.
	 */
	public abstract int getDataBits();

	/**
	 * Gets the flow control mode (int) value.
	 * @return	The flow control mode (<code>int</code>) value.
	 * @see #setFlowControlMode(int)
	 */
	public abstract int getFlowControlMode();

	/**
	 * Gets the parity (int) value.
	 * @return	The parity (<code>int</code>) value.
	 */
	public abstract int getParity();

	/**
	 * Gets the stop bits (int) value.
	 * @return	The stop bits (<code>int</code>) value.
	 */
	public abstract int getStopBits();

	/**
	 * Gets the cd (boolean) value.
	 * @return	The cd (<code>boolean</code>) value.
	 */
	public abstract boolean isCD();

	/**
	 * Gets the cts (boolean) value.
	 * @return	The cts (<code>boolean</code>) value.
	 * @see #notifyOnCTS(boolean)
	 */
	public abstract boolean isCTS();

	/**
	 * Gets the dsr (boolean) value.
	 * @return	The dsr (<code>boolean</code>) value.
	 * @see #notifyOnDSR(boolean)
	 */
	public abstract boolean isDSR();

	/**
	 * Gets the dtr (boolean) value.
	 * @return	The dtr (<code>boolean</code>) value.
	 * @see #setDTR(boolean)
	 */
	public abstract boolean isDTR();

	/**
	 * Gets the ri (boolean) value.
	 * @return	The ri (<code>boolean</code>) value.
	 */
	public abstract boolean isRI();

	/**
	 * Gets the rts (boolean) value.
	 * @return	The rts (<code>boolean</code>) value.
	 * @see #setRTS(boolean)
	 */
	public abstract boolean isRTS();

	/**
	 * Notify on break interrupt with the specified enable parameter.
	 * @param enable	The enable (<code>boolean</code>) parameter.
	 */
	public abstract void notifyOnBreakInterrupt(final boolean enable);

	/**
	 * Notify on cts with the specified enable parameter.
	 * @param enable	The enable (<code>boolean</code>) parameter.
	 */
	public abstract void notifyOnCTS(final boolean enable);

	/**
	 * Notify on carrier detect with the specified enable parameter.
	 * @param enable	The enable (<code>boolean</code>) parameter.
	 */
	public abstract void notifyOnCarrierDetect(final boolean enable);

	/**
	 * Notify on dsr with the specified enable parameter.
	 * @param enable	The enable (<code>boolean</code>) parameter.
	 */
	public abstract void notifyOnDSR(final boolean enable);

	/**
	 * Notify on data available with the specified enable parameter.
	 * @param enable	The enable (<code>boolean</code>) parameter.
	 */
	public abstract void notifyOnDataAvailable(final boolean enable);

	/**
	 * Notify on framing error with the specified enable parameter.
	 * @param enable	The enable (<code>boolean</code>) parameter.
	 */
	public abstract void notifyOnFramingError(final boolean enable);

	/**
	 * Notify on output empty with the specified enable parameter.
	 * @param enable	The enable (<code>boolean</code>) parameter.
	 */
	public abstract void notifyOnOutputEmpty(final boolean enable);

	/**
	 * Notify on overrun error with the specified enable parameter.
	 * @param enable	The enable (<code>boolean</code>) parameter.
	 */
	public abstract void notifyOnOverrunError(final boolean enable);

	/**
	 * Notify on parity error with the specified enable parameter.
	 * @param enable	The enable (<code>boolean</code>) parameter.
	 */
	public abstract void notifyOnParityError(final boolean enable);

	/**
	 * Notify on ring indicator with the specified enable parameter.
	 * @param enable	The enable (<code>boolean</code>) parameter.
	 */
	public abstract void notifyOnRingIndicator(final boolean enable);

	/**
	 * Remove event listener.
	 * @see #addEventListener(SerialPortEventListener)
	 */
	public abstract void removeEventListener();

	/**
	 * Send break with the specified millis parameter.
	 * @param millis	The millis (<code>int</code>) parameter.
	 */
	public abstract void sendBreak(final int millis);

	/**
	 * Sets the dtr value.
	 * @param dtr	The dtr (<code>boolean</code>) parameter.
	 * @see #isDTR()
	 */
	public abstract void setDTR(final boolean dtr);

	/**
	 * Sets the flow control mode value.
	 * @param flowcontrol	The flowcontrol (<code>int</code>) parameter.
	 * @throws UnsupportedCommOperationException Unsupported Comm Operation Exception.
	 * @see #getFlowControlMode()
	 */
	public abstract void setFlowControlMode(final int flowcontrol) throws UnsupportedCommOperationException;

	/**
	 * Sets the rts value.
	 * @param rts	The rts (<code>boolean</code>) parameter.
	 * @see #isRTS()
	 */
	public abstract void setRTS(final boolean rts);

	/**
	 * Sets the rcv fifo trigger value.
	 * @param trigger	The trigger (<code>int</code>) parameter.
	 */
	public void setRcvFifoTrigger(final int trigger) {
		/* do nothing */
	}

	/**
	 * Set serial port params with the specified baudrate, data bits, stop bits and parity parameters.
	 * @param baudrate	The baudrate (<code>int</code>) parameter.
	 * @param dataBits	The data bits (<code>int</code>) parameter.
	 * @param stopBits	The stop bits (<code>int</code>) parameter.
	 * @param parity	The parity (<code>int</code>) parameter.
	 * @throws UnsupportedCommOperationException Unsupported Comm Operation Exception.
	 */
	public abstract void setSerialPortParams(final int baudrate, final int dataBits, final int stopBits, final int parity) throws UnsupportedCommOperationException;
}
