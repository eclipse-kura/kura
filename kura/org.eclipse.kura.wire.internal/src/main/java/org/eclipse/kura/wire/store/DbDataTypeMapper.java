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

package org.eclipse.kura.wire.store;

import java.sql.Types;
import java.util.Map;

import org.eclipse.kura.type.DataType;

import com.google.common.collect.Maps;

/**
 * The Class DbDataTypeMapper maps all the Kura specific data types to JDBC Data
 * Types
 */
public final class DbDataTypeMapper {

	/**
	 * The Class JdbcType.
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
		public JdbcType(final int type, final String typeStr) {
			this.m_type = type;
			this.m_typeStr = typeStr;
		}

		/**
		 * Gets the type.
		 *
		 * @return the type
		 */
		public int getType() {
			return this.m_type;
		}

		/**
		 * Gets the type string.
		 *
		 * @return the type string
		 */
		public String getTypeString() {
			return this.m_typeStr;
		}
	}

	/**
	 * The map containing key-value pairs of the Kura Datatype and the JDBC
	 * datatype
	 */
	private static Map<DataType, JdbcType> s_dataTypeMap = Maps.newHashMap();

	/** The s_jdbc type map. */
	private static Map<Integer, DataType> s_jdbcTypeMap = Maps.newHashMap();

	static {
		s_dataTypeMap.put(DataType.BYTE, new JdbcType(Types.TINYINT, "TINYINT"));
		s_dataTypeMap.put(DataType.SHORT, new JdbcType(Types.SMALLINT, "SMALLINT"));
		s_dataTypeMap.put(DataType.INTEGER, new JdbcType(Types.INTEGER, "INTEGER"));
		s_dataTypeMap.put(DataType.LONG, new JdbcType(Types.BIGINT, "BIGINT"));
		s_dataTypeMap.put(DataType.DOUBLE, new JdbcType(Types.DOUBLE, "DOUBLE"));
		s_dataTypeMap.put(DataType.BOOLEAN, new JdbcType(Types.BOOLEAN, "BOOLEAN"));
		s_dataTypeMap.put(DataType.BYTE_ARRAY, new JdbcType(Types.BINARY, "BINARY"));
		s_dataTypeMap.put(DataType.STRING, new JdbcType(Types.VARCHAR, "VARCHAR(102400)"));
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

	/**
	 * Not needed because of utility class
	 */
	private DbDataTypeMapper() {
		// Not needed
	}
}
