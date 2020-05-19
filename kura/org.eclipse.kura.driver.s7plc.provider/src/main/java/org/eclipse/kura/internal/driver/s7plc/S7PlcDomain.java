/**
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates
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
    private final int area;

    public S7PlcDomain(int db, int area) {
        this.db = db;
        this.area = area;
    }

    public int getDB() {
        return this.db;
    }

    public int getArea() {
        return this.area;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.area;
        result = prime * result + this.db;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        S7PlcDomain other = (S7PlcDomain) obj;
        if (this.area != other.area) {
            return false;
        }
        if (this.db != other.db) {
            return false;
        }
        return true;
    }

}
