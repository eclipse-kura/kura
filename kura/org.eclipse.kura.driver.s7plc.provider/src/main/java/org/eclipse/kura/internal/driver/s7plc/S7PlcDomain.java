/**
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 */

package org.eclipse.kura.internal.driver.s7plc;

public class S7PlcDomain {

    private final int db;

    public S7PlcDomain(int db) {
        this.db = db;
    }

    public int getDB() {
        return this.db;
    }

    @Override
    public int hashCode() {
        return this.db;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        S7PlcDomain other = (S7PlcDomain) obj;
        if (this.db != other.db) {
            return false;
        }
        return true;
    }
}
