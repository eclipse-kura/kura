/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *******************************************************************************/

package org.eclipse.kura.internal.wire.store;

import static org.eclipse.kura.type.DataType.BOOLEAN;
import static org.eclipse.kura.type.DataType.BYTE;
import static org.eclipse.kura.type.DataType.BYTE_ARRAY;
import static org.eclipse.kura.type.DataType.DOUBLE;
import static org.eclipse.kura.type.DataType.INTEGER;
import static org.eclipse.kura.type.DataType.LONG;
import static org.eclipse.kura.type.DataType.SHORT;
import static org.eclipse.kura.type.DataType.STRING;

import java.sql.Types;
import java.util.Map;

import org.eclipse.kura.type.DataType;
import org.eclipse.kura.util.collection.CollectionUtil;

/**
 * The Class DbDataTypeMapper maps all the Kura specific data types to JDBC Data
 * Types
 */
public final class DbDataTypeMapper {

    /**
     * The Class JdbcType represent a pair of the generic SQL Type and its
     * string representation
     */
    public static class JdbcType {

        /** The JDBC type represented as integer. */
        private final int type;

        /** The JDBC type represented as string. */
        private final String typeStr;

        /**
         * Instantiates a new JDBC type.
         *
         * @param type
         *            the type
         * @param typeStr
         *            the type string
         */
        JdbcType(final int type, final String typeStr) {
            this.type = type;
            this.typeStr = typeStr;
        }

        /**
         * Gets the type.
         *
         * @return the type
         */
        int getType() {
            return this.type;
        }

        /**
         * Gets the type string.
         *
         * @return the type string
         */
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
        dataTypeMap.put(BYTE, new JdbcType(Types.TINYINT, "TINYINT"));
        dataTypeMap.put(SHORT, new JdbcType(Types.SMALLINT, "SMALLINT"));
        dataTypeMap.put(INTEGER, new JdbcType(Types.INTEGER, "INTEGER"));
        dataTypeMap.put(LONG, new JdbcType(Types.BIGINT, "BIGINT"));
        dataTypeMap.put(DOUBLE, new JdbcType(Types.DOUBLE, "DOUBLE"));
        dataTypeMap.put(BOOLEAN, new JdbcType(Types.BOOLEAN, "BOOLEAN"));
        dataTypeMap.put(BYTE_ARRAY, new JdbcType(Types.BINARY, "BINARY"));
        dataTypeMap.put(STRING, new JdbcType(Types.VARCHAR, "VARCHAR(102400)"));
    }

    static {
        jdbcTypeMap.put(Types.TINYINT, DataType.BYTE);
        jdbcTypeMap.put(Types.SMALLINT, DataType.SHORT);
        jdbcTypeMap.put(Types.INTEGER, DataType.INTEGER);
        jdbcTypeMap.put(Types.BIGINT, DataType.LONG);
        jdbcTypeMap.put(Types.DOUBLE, DataType.DOUBLE);
        jdbcTypeMap.put(Types.BOOLEAN, DataType.BOOLEAN);
        jdbcTypeMap.put(Types.BINARY, DataType.BYTE_ARRAY);
        jdbcTypeMap.put(Types.VARCHAR, DataType.STRING);
    }

    /**
     * Constructor
     */
    private DbDataTypeMapper() {
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
