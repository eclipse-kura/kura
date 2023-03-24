/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.network.status.provider.api;

import java.util.Set;

import org.eclipse.kura.net.status.modem.ModemMode;
import org.eclipse.kura.net.status.modem.ModemModePair;

@SuppressWarnings("unused")
public class ModemModePairDTO {

    private final Set<ModemMode> modes;
    private final ModemMode preferredMode;

    public ModemModePairDTO(final ModemModePair modemModePair) {
        this.modes = modemModePair.getModes();
        this.preferredMode = modemModePair.getPreferredMode();
    }
}
