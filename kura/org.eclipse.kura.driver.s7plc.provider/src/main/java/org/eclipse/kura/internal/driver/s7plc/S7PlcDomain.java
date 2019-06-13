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

    private final int dbType;
    private final int db;

    public S7PlcDomain(int dbType, int db) {
        this.db = db;
        this.dbType = dbType;
    }

    public int getDB() {
        return db;
    }

    public int getDbType() {
        return dbType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + db;
        result = prime * result + dbType;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        S7PlcDomain other = (S7PlcDomain) obj;
        if (db != other.db)
            return false;
        if (dbType != other.dbType)
            return false;
        return true;
    }

}
