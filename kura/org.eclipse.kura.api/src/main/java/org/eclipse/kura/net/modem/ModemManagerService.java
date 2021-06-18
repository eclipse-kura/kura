/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.net.modem;

import java.util.Collection;
import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.annotation.versioning.ProviderType;

/**
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface ModemManagerService {

    /**
     * Returns the modem tracked with the given id.
     * 
     * @deprecated since 2.2 use {@link ModemManagerService#withModemService(String, Function)} instead
     * @param id
     *            The id of the modem, in case of an USB modem, the id is the USB port as returned by
     *            {@link org.eclipse.kura.usb.UsbModemDevice#getUsbPort()}, in case of a serial modem the id is the
     *            product name as returned by {@link org.eclipse.kura.usb.UsbModemDevice#getProductName()}
     * @return The cellular modem instance, or {@code null} if a modem with the given id is not currently tracked
     */
    @Deprecated
    public CellularModem getModemService(String id);

    /**
     * Returns the list of currently tracked modems
     * 
     * @deprecated since 2.2 use {@link ModemManagerService#withAllModemServices(Function)} instead
     * @return the list of currently tracked modems
     */
    @Deprecated
    public Collection<CellularModem> getAllModemServices();

    /**
     * Applies the provided function to the modem named {@code id}.
     * The function will have exclusive access to the modem until it returns.
     * 
     * @since 2.2
     * 
     * @param <T>
     *            The return type of the function
     * @param id
     *            The id of the modem, in case of an USB modem, the id is the USB port as returned by
     *            {@link org.eclipse.kura.usb.UsbModemDevice#getUsbPort()}, in case of a serial modem the id is the
     *            product name as returned by {@link org.eclipse.kura.usb.UsbModemDevice#getProductName()}
     * @param func
     * @return The result of the provided function applied to the modem
     */
    public <T> T withModemService(String id, ModemFunction<Optional<CellularModem>, T> func) throws KuraException;

    /**
     * Applies the provided function to all currently tracked modems.
     * The function will have exclusive access to the modems until it returns.
     * 
     * 
     * @since 2.2
     * 
     * @param <T>
     *            The return type of the function
     * @param func
     *            The function to be called
     * @return The result of the provided function applied to the modem
     */
    public <T> T withAllModemServices(ModemFunction<Collection<CellularModem>, T> func) throws KuraException;

    /**
     * 
     * @since 2.2
     * 
     * @param <T>
     * @param <U>
     */
    @ConsumerType
    public interface ModemFunction<T, U> {

        public U apply(final T arg) throws KuraException;
    }
}
