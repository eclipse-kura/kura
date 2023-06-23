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
 ******************************************************************************/
package org.eclipse.kura.net.status.modem;

import java.util.Objects;
import java.util.Set;

/**
 * This class represents a pair of Modem Mode list and a preferred one.
 *
 */
public class ModemModePair {

    private final Set<ModemMode> modes;
    private final ModemMode preferredMode;

    public ModemModePair(Set<ModemMode> modes, ModemMode preferredMode) {
        this.modes = modes;
        this.preferredMode = preferredMode;
    }

    public Set<ModemMode> getModes() {
        return this.modes;
    }

    public ModemMode getPreferredMode() {
        return this.preferredMode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.modes, this.preferredMode);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ModemModePair other = (ModemModePair) obj;
        return Objects.equals(this.modes, other.modes) && this.preferredMode == other.preferredMode;
    }

}
