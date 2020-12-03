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
package org.eclipse.kura.internal.driver.opcua;

public enum VariableType {
    DEFINED_BY_JAVA_TYPE,
    BOOLEAN,
    SBYTE,
    INT16,
    INT32,
    INT64,
    BYTE,
    UINT16,
    UINT32,
    UINT64,
    FLOAT,
    DOUBLE,
    STRING,
    BYTE_STRING,
    BYTE_ARRAY,
    SBYTE_ARRAY
}
