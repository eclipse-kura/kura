/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.internal.wire.h2db.store;

import static org.eclipse.kura.type.DataType.BOOLEAN;
import static org.eclipse.kura.type.DataType.BYTE_ARRAY;
import static org.eclipse.kura.type.DataType.DOUBLE;
import static org.eclipse.kura.type.DataType.FLOAT;
import static org.eclipse.kura.type.DataType.INTEGER;
import static org.eclipse.kura.type.DataType.LONG;
import static org.eclipse.kura.type.DataType.STRING;

import java.sql.Types;
import java.util.Map;

import org.eclipse.kura.type.DataType;
import org.eclipse.kura.util.collection.CollectionUtil;

/**
 * This class maps all the Kura specific data types to JDBC Data Types
 */
public final class H2DbDataTypeMapper {

    /**
     * The Class JdbcType represent a pair of the generic SQL Type and its
     * string representation
     */
    public static class JdbcType {

        private final int type;

        private final String typeStr;

        JdbcType(final int type, final String typeStr) {
            this.type = type;
            this.typeStr = typeStr;
        }

        int getType() {
            return this.type;
        }

        String getTypeString() {
            return this.typeStr;
        }
    }

    /**
     * The map containing key-value pairs of the Kura Datatype and the JDBC
     * datatype
     */
    private static Map<DataType, JdbcType> dataTypeMap = CollectionUtil.newHashMap();

    /** The JDBC Type Holder map. */
    private static Map<Integer, DataType> jdbcTypeMap = CollectionUtil.newHashMap();

    static {
        dataTypeMap.put(FLOAT, new JdbcType(Types.FLOAT, "FLOAT"));
        dataTypeMap.put(INTEGER, new JdbcType(Types.INTEGER, "INTEGER"));
        dataTypeMap.put(LONG, new JdbcType(Types.BIGINT, "BIGINT"));
        dataTypeMap.put(DOUBLE, new JdbcType(Types.DOUBLE, "DOUBLE"));
        dataTypeMap.put(BOOLEAN, new JdbcType(Types.BOOLEAN, "BOOLEAN"));
        dataTypeMap.put(BYTE_ARRAY, new JdbcType(Types.BLOB, "BLOB"));
        dataTypeMap.put(STRING, new JdbcType(Types.VARCHAR, "VARCHAR(102400)"));
    }

    static {
        jdbcTypeMap.put(Types.FLOAT, DataType.FLOAT);
        jdbcTypeMap.put(Types.INTEGER, DataType.INTEGER);
        jdbcTypeMap.put(Types.BIGINT, DataType.LONG);
        jdbcTypeMap.put(Types.DOUBLE, DataType.DOUBLE);
        jdbcTypeMap.put(Types.BOOLEAN, DataType.BOOLEAN);
        jdbcTypeMap.put(Types.BLOB, DataType.BYTE_ARRAY);
        jdbcTypeMap.put(Types.VARCHAR, DataType.STRING);
    }

    private H2DbDataTypeMapper() {
        // Not needed
    }

    /**
     * Gets the data type.
     *
     * @param jdbcType
     *            the JDBC type
     * @return the data type
     */
    public static DataType getDataType(final int jdbcType) {
        return jdbcTypeMap.get(jdbcType);
    }

    /**
     * Gets the JDBC type.
     *
     * @param dataType
     *            the data type
     * @return the JDBC type
     */
    public static JdbcType getJdbcType(final DataType dataType) {
        return dataTypeMap.get(dataType);
    }
}
