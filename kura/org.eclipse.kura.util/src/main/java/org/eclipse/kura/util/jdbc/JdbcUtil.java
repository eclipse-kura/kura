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
package org.eclipse.kura.util.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class JdbcUtil {

    private JdbcUtil() {
    }

    public static <T> Optional<T> getFirstColumnValueOrEmpty(final SQLSupplier<ResultSet> resultSet,
            final SQLBiFunction<ResultSet, Integer, T> extractor) throws SQLException {
        try (final ResultSet rs = resultSet.get()) {
            if (rs.next()) {
                return Optional.of(extractor.call(rs, 1));
            } else {
                return Optional.empty();
            }
        }
    }

    public static <T> T getFirstColumnValue(final SQLSupplier<ResultSet> resultSet,
            final SQLBiFunction<ResultSet, Integer, T> extractor) throws SQLException {
        return getFirstColumnValueOrEmpty(resultSet, extractor).orElseThrow(() -> new SQLException("empty result set"));
    }
}
