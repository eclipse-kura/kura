/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal (admin@amitinside.com)
 */

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
		private final int m_type;

		/** The JDBC type represented as string. */
		private final String m_typeStr;

		/**
		 * Instantiates a new JDBC type.
		 *
		 * @param type
		 *            the type
		 * @param typeStr
		 *            the type string
		 */
		JdbcType(final int type, final String typeStr) {
			this.m_type = type;
			this.m_typeStr = typeStr;
		}

		/**
		 * Gets the type.
		 *
		 * @return the type
		 */
		int getType() {
			return this.m_type;
		}

		/**
		 * Gets the type string.
		 *
		 * @return the type string
		 */
		String getTypeString() {
			return this.m_typeStr;
		}
	}

	/**
	 * The map containing key-value pairs of the Kura Datatype and the JDBC
	 * datatype
	 */
	private static Map<DataType, JdbcType> s_dataTypeMap = CollectionUtil.newHashMap();

	/** The JDBC Type Holder map. */
	private static Map<Integer, DataType> s_jdbcTypeMap = CollectionUtil.newHashMap();

	static {
		s_dataTypeMap.put(BYTE, new JdbcType(Types.TINYINT, "TINYINT"));
		s_dataTypeMap.put(SHORT, new JdbcType(Types.SMALLINT, "SMALLINT"));
		s_dataTypeMap.put(INTEGER, new JdbcType(Types.INTEGER, "INTEGER"));
		s_dataTypeMap.put(LONG, new JdbcType(Types.BIGINT, "BIGINT"));
		s_dataTypeMap.put(DOUBLE, new JdbcType(Types.DOUBLE, "DOUBLE"));
		s_dataTypeMap.put(BOOLEAN, new JdbcType(Types.BOOLEAN, "BOOLEAN"));
		s_dataTypeMap.put(BYTE_ARRAY, new JdbcType(Types.BINARY, "BINARY"));
		s_dataTypeMap.put(STRING, new JdbcType(Types.VARCHAR, "VARCHAR(102400)"));
	}

	static {
		s_jdbcTypeMap.put(Types.TINYINT, DataType.BYTE);
		s_jdbcTypeMap.put(Types.SMALLINT, DataType.SHORT);
		s_jdbcTypeMap.put(Types.INTEGER, DataType.INTEGER);
		s_jdbcTypeMap.put(Types.BIGINT, DataType.LONG);
		s_jdbcTypeMap.put(Types.DOUBLE, DataType.DOUBLE);
		s_jdbcTypeMap.put(Types.BOOLEAN, DataType.BOOLEAN);
		s_jdbcTypeMap.put(Types.BINARY, DataType.BYTE_ARRAY);
		s_jdbcTypeMap.put(Types.VARCHAR, DataType.STRING);
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
	 *            the jdbc type
	 * @return the data type
	 */
	public static DataType getDataType(final int jdbcType) {
		return s_jdbcTypeMap.get(jdbcType);
	}

	/**
	 * Gets the JDBC type.
	 *
	 * @param dataType
	 *            the data type
	 * @return the JDBC type
	 */
	public static JdbcType getJdbcType(final DataType dataType) {
		return s_dataTypeMap.get(dataType);
	}

}
