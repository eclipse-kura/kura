/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.driver;

import static org.eclipse.kura.Preconditions.checkNull;

import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.annotation.Immutable;
import org.eclipse.kura.annotation.Nullable;
import org.eclipse.kura.annotation.ThreadSafe;

/**
 * The Class DriverStatus is responsible for representing the status of any
 * driver specific operation
 */
@Immutable
@ThreadSafe
public final class DriverStatus {

	/** The driver flag. */
	private final DriverFlag driverFlag;

	/** The exception instance if needed. */
	@Nullable
	private final Exception exception;

	/** The exception Message. */
	@Nullable
	private final String exceptionMessage;

	/**
	 * Instantiates a new status.
	 *
	 * @param driverFlag
	 *            the driver flag
	 * @param exceptionMessage
	 *            the exception message
	 * @param exception
	 *            the exception
	 * @throws KuraRuntimeException
	 *             if the driver flag is null
	 */
	public DriverStatus(final DriverFlag driverFlag, @Nullable final String exceptionMessage,
			@Nullable final Exception exception) {
		checkNull(driverFlag, "Driver Flag cannot be null");
		this.driverFlag = driverFlag;
		this.exceptionMessage = exceptionMessage;
		this.exception = exception;
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		final DriverStatus other = (DriverStatus) obj;
		if (this.driverFlag != other.driverFlag) {
			return false;
		}
		if (this.exception == null) {
			if (other.exception != null) {
				return false;
			}
		} else if (!this.exception.equals(other.exception)) {
			return false;
		}
		if (this.exceptionMessage == null) {
			if (other.exceptionMessage != null) {
				return false;
			}
		} else if (!this.exceptionMessage.equals(other.exceptionMessage)) {
			return false;
		}
		return true;
	}

	/**
	 * Gets the driver flag.
	 *
	 * @return the driver flag
	 */
	public DriverFlag getDriverFlag() {
		return this.driverFlag;
	}

	/**
	 * Gets the exception.
	 *
	 * @return the exception
	 */
	public Exception getException() {
		return this.exception;
	}

	/**
	 * Gets the exception message.
	 *
	 * @return the exception message
	 */
	public String getExceptionMessage() {
		return this.exceptionMessage;
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((this.driverFlag == null) ? 0 : this.driverFlag.hashCode());
		result = (prime * result) + ((this.exception == null) ? 0 : this.exception.hashCode());
		result = (prime * result) + ((this.exceptionMessage == null) ? 0 : this.exceptionMessage.hashCode());
		return result;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return "Status [driverFlag=" + this.driverFlag + ", exception=" + this.exception + ", exceptionMessage="
				+ this.exceptionMessage + "]";
	}

}
