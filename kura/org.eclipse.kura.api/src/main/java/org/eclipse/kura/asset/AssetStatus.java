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
package org.eclipse.kura.asset;

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
public final class AssetStatus {

	/** The asset flag. */
	private final AssetFlag assetFlag;

	/** The exception instance if needed. */
	@Nullable
	private final Exception exception;

	/** The exception Message. */
	@Nullable
	private final String exceptionMessage;

	/**
	 * Instantiates a new status.
	 *
	 * @param assetFlag
	 *            the asset flag
	 * @throws KuraRuntimeException
	 *             if the asset flag is null
	 */
	public AssetStatus(final AssetFlag assetFlag) {
		checkNull(assetFlag, "Asset Flag cannot be null");
		this.assetFlag = assetFlag;
		this.exceptionMessage = null;
		this.exception = null;
	}

	/**
	 * Instantiates a new status.
	 *
	 * @param assetFlag
	 *            the asset flag
	 * @param exceptionMessage
	 *            the exception message
	 * @param exception
	 *            the exception
	 * @throws KuraRuntimeException
	 *             if the asset flag is null
	 */
	public AssetStatus(final AssetFlag assetFlag, @Nullable final String exceptionMessage,
			@Nullable final Exception exception) {
		checkNull(assetFlag, "Driver Flag cannot be null");
		this.assetFlag = assetFlag;
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
		final AssetStatus other = (AssetStatus) obj;
		if (this.assetFlag != other.assetFlag) {
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
	 * Gets the asset flag.
	 *
	 * @return the asset flag
	 */
	public AssetFlag getAssetFlag() {
		return this.assetFlag;
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
		result = (prime * result) + ((this.assetFlag == null) ? 0 : this.assetFlag.hashCode());
		result = (prime * result) + ((this.exception == null) ? 0 : this.exception.hashCode());
		result = (prime * result) + ((this.exceptionMessage == null) ? 0 : this.exceptionMessage.hashCode());
		return result;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return "Status [driverFlag=" + this.assetFlag + ", exception=" + this.exception + ", exceptionMessage="
				+ this.exceptionMessage + "]";
	}

}
