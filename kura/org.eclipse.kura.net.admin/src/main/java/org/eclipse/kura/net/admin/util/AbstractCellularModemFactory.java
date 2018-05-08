/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.admin.util;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.kura.net.admin.modem.CellularModemFactory;
import org.eclipse.kura.net.modem.CellularModem;
import org.eclipse.kura.net.modem.ModemDevice;

public abstract class AbstractCellularModemFactory<T extends CellularModem> implements CellularModemFactory {

    protected Map<ModemDevice, T> cellularModems = new HashMap<>();

    protected abstract T createCellularModem(ModemDevice modemDevice, String platform) throws Exception;

    protected void shutdownCellularModem(T modem) {
    }

    @Override
    public synchronized CellularModem obtainCellularModemService(ModemDevice modemDevice, String platform)
            throws Exception {

        final CellularModem modem = cellularModems.get(modemDevice);

        if (modem != null) {
            return modem;
        }

        final T newModemInstance = createCellularModem(modemDevice, platform);
        requireNonNull(newModemInstance);

        cellularModems.put(modemDevice, newModemInstance);
        return newModemInstance;
    }

    @Override
    public synchronized void releaseModemService(CellularModem modem) {
        final Optional<Entry<ModemDevice, T>> entryOptional = cellularModems.entrySet().stream()
                .filter(e -> e.getValue() == modem).findAny();

        if (!entryOptional.isPresent()) {
            return;
        }

        final Entry<ModemDevice, T> entry = entryOptional.get();

        shutdownCellularModem(entry.getValue());
        cellularModems.entrySet().remove(entry);
    }

    @Override
    public synchronized Map<ModemDevice, CellularModem> getAllModemServices() {
        return new HashMap<>(cellularModems);
    }

    @Override
    public void releaseModemService(String usbPortAddress) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Hashtable<String, ? extends CellularModem> getModemServices() {
        final Map<ModemDevice, T> cellularModems;

        synchronized (this) {
            cellularModems = new HashMap<>(this.cellularModems);
        }

        return cellularModems.entrySet().stream().collect(
                Collectors.toMap(e -> e.getKey().getProductName(), Entry::getValue, (a, b) -> b, Hashtable::new));
    }
}
