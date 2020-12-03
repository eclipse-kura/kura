/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.linux.net.modem;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.internal.board.BoardPowerState;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.usb.UsbModemDevice;

/**
 * Provides methods to manage the modem status, allowing to turn off, on or reset the modems identified by the provided
 * vendor and product ids.
 *
 */
public interface GatewayModemDriver {

    /**
     * Enables the resource specified by the passed parameters
     *
     * @param vendor
     * @param product
     * @throws KuraException
     * @deprecated use the {@link #enable(ModemDevice modemDevice)} method
     */
    @Deprecated
    public void enable(String vendor, String product) throws KuraException;

    /**
     * Disables the resource specified by the passed parameters
     *
     * @param vendor
     * @param product
     * @throws KuraException
     * @deprecated use the {@link #disable(ModemDevice modemDevice)} method
     */
    @Deprecated
    public void disable(String vendor, String product) throws KuraException;

    /**
     * Resets the resource specified by the passed parameters
     *
     * @param vendor
     * @param product
     * @throws KuraException
     * @deprecated use the {@link #reset(ModemDevice modemDevice)} method
     */
    @Deprecated
    public void reset(String vendor, String product) throws KuraException;

    /**
     * Returns the state of the resource specified by the passed parameters
     *
     * @param vendor
     * @param product
     * @return a {@link BoardPowerState} representing the current status
     * @throws KuraException
     * @deprecated use the {@link #getState(ModemDevice modemDevice)} method
     */
    @Deprecated
    public BoardPowerState getState(String vendor, String product) throws KuraException;

    /**
     * Enables the resource specified by the passed parameters
     *
     * @param modemDevice
     * @throws KuraException
     * @since 1.1
     */
    public default void enable(ModemDevice modemDevice) throws KuraException {
        if (modemDevice instanceof UsbModemDevice) {
            enable(((UsbModemDevice) modemDevice).getVendorId(), ((UsbModemDevice) modemDevice).getProductId());
        } else {
            throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
        }
    }

    /**
     * Disables the resource specified by the passed parameters
     *
     * @param modemDevice
     * @throws KuraException
     * @since 1.1
     */
    public default void disable(ModemDevice modemDevice) throws KuraException {
        if (modemDevice instanceof UsbModemDevice) {
            disable(((UsbModemDevice) modemDevice).getVendorId(), ((UsbModemDevice) modemDevice).getProductId());
        } else {
            throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
        }
    }

    /**
     * Resets the resource specified by the passed parameters
     *
     * @param modemDevice
     * @throws KuraException
     * @since 1.1
     */
    public default void reset(ModemDevice modemDevice) throws KuraException {
        if (modemDevice instanceof UsbModemDevice) {
            reset(((UsbModemDevice) modemDevice).getVendorId(), ((UsbModemDevice) modemDevice).getProductId());
        } else {
            throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
        }
    }

    /**
     * Returns the state of the resource specified by the passed parameters
     *
     * @param modemDevice
     * @return a {@link BoardPowerState} representing the current status
     * @throws KuraException
     * @since 1.1
     */
    public default BoardPowerState getState(ModemDevice modemDevice) throws KuraException {
        if (modemDevice instanceof UsbModemDevice) {
            return getState(((UsbModemDevice) modemDevice).getVendorId(),
                    ((UsbModemDevice) modemDevice).getProductId());
        } else {
            throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
        }
    }
}
