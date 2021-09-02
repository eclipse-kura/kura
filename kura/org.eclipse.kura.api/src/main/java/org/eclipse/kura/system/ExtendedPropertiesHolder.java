/*******************************************************************************
 * Copyright (c) 2021 WinWinIt and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  WinWinIt
 *******************************************************************************/
package org.eclipse.kura.system;

import java.util.List;

/**
 * <p>
 * A MQTT birth certificate extended properties holder.
 * </p>
 * <p>
 * Every time a birth certificate has to be sent, Kura retrieves extended properties from all
 * {@link ExtendedPropertiesHolder} implementations, merging the properties and adding them to the birth certificate.
 * </p>
 * <p>
 * A concrete implementation must implement the {@link #getProperties()} method, returning a flat list of
 * {@link Property}.
 * </p>
 */
public interface ExtendedPropertiesHolder {

    /**
     * A MQTT birth certificate extended property.
     */
    public static class Property {

        /** Property group name. */
        private final String groupName;
        /** Property name. */
        private final String name;
        /** Property value. */
        private final String value;

        /**
         * Constructor.
         * 
         * @param groupName
         *                      Property group name,
         * @param name
         *                      Property name.
         * @param value
         *                      Property value.
         */
        public Property(final String groupName, final String name, final String value) {
            super();
            this.groupName = groupName;
            this.name = name;
            this.value = value;
        }

        /**
         * Returns the extended property group.
         * 
         * @return Group name.
         */
        public final String getGroupName() {
            return groupName;
        }

        /**
         * Returns the extended property name.
         * 
         * @return Property name.
         */
        public final String getName() {
            return name;
        }

        /**
         * Returns the extended property value.
         * 
         * @return Property value.
         */
        public final String getValue() {
            return value;
        }

    }

    /**
     * Returns the MQTT birth certificate extended properties provided by the concrete implementation.
     * 
     * @return List of properties, or {@literal null} if none available.
     */
    List<Property> getProperties();

}
