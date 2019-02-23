/**
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */

package org.eclipse.kura.internal.driver.s7plc;

public class S7PlcDomain {

    private final int db;

    public S7PlcDomain(int db) {
        this.db = db;
    }

    public int getDB() {
        return db;
    }

    @Override
    public int hashCode() {
        return db;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        S7PlcDomain other = (S7PlcDomain) obj;
        if (db != other.db)
            return false;
        return true;
    }
}
