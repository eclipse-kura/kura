/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.oracle.dio.utils;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;
import java.util.Enumeration;

public final class Configuration {

    /** Don't let anyone instantiate this class */
    private Configuration() {
    }

    /**
     * Gets the implementation property indicated by the specified key.
     *
     * @param      key   the name of the implementation property.
     * @return     the string value of the implementation property,
     *             or <code>null</code> if there is no property with that key.
     *
     * @exception  NullPointerException if <code>key</code> is
     *             <code>null</code>.
     * @exception  IllegalArgumentException if <code>key</code> is empty.
     */
    public static String getProperty(String key) {
        return System.getProperty(key);
    }

    /**
     * Returns system property value by the given key using a privileged call.
     *
     * @param key property key
     * @return property value
     */
    public static String getSystemProperty(final String key) {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
                public String run() {
                    return System.getProperty(key);
                }
            });
    }

    /**
     * Gets the implementation property indicated by the specified key or
     * returns the specified default value.
     *
     * @param      key   the name of the implementation property.
     * @param      def   the default value for the property if not
     *                  specified in the configuration files or command
     *                  line over rides.
     * @return     the string value of the implementation property,
     *             or <code>def</code> if there is no property with that key.
     *
     * @exception  NullPointerException if <code>key</code> is
     *             <code>null</code>.
     * @exception  IllegalArgumentException if <code>key</code> is empty.
     */
    public static String getPropertyDefault(String key, String def) {
        return System.getProperty(key, def);
    }

    /**
     * Gets the implementation property indicated by the specified key or
     * returns the specified default value as an positive int.
     *
     * @param      key   the name of the implementation property.
     * @param      def   the default value for the property if not
     *                  specified in the configuration files or command
     *                  line over rides.
     *
     * @return     the int value of the implementation property,
     *             or <code>def</code> if there is no property with that key or
     *             the config value is not a positive int (zero is not
     *             positive).
     *
     * @exception  NullPointerException if <code>key</code> is
     *             <code>null</code>.
     * @exception  IllegalArgumentException if <code>key</code> is empty.
     */
    public static int getPositiveIntProperty(String key, int def) {
        int value = Configuration.getIntProperty(key, def);
        return (value > 0 ? value : def);
    }

    /**
     * Gets the implementation property indicated by the specified key or
     * returns the specified default value as an nonzero int.
     *
     * @param      key   the name of the implementation property.
     * @param      def   the default value for the property if not
     *                  specified in the configuration files or command
     *                  line over rides.
     * @return     the int value of the implementation property,
     *             or <code>def</code> if there is no property with that key or
     *             the config value is not an int.
     *
     * @exception  NullPointerException if <code>key</code> is
     *             <code>null</code>.
     * @exception  IllegalArgumentException if <code>key</code> is empty.
     */
    public static int getNonNegativeIntProperty(String key, int def) {
        int value = Configuration.getIntProperty(key, def);
        return (value >= 0 ? value : def);
    }

    /**
     * Gets the implementation property indicated by the specified key or
     * returns the specified default value as an int.
     *
     * @param      key   the name of the implementation property.
     * @param      def   the default value for the property if not
     *                  specified in the configuration files or command
     *                  line over rides.
     *
     * @return     the int value of the implementation property,
     *             or <code>def</code> if there is no property with that key or
     *             the config value is not an int.
     *
     * @exception  NullPointerException if <code>key</code> is
     *             <code>null</code>.
     * @exception  IllegalArgumentException if <code>key</code> is empty.
     */
    public static int getIntProperty(String key, int def) {
        String value = System.getProperty(key);
        if (value != null) {
            try {
                return Integer.valueOf(value).intValue();
            } catch (NumberFormatException e) {
            }
        }
        return def;
    }

    /**
     * Gets the implementation property indicated by the specified key or
     * returns the specified default value as a boolean.
     *
     * @param      key   the name of the implementation property.
     * @param      def   the default value for the property if not
     *                  specified in the configuration files or command
     *                  line over rides.
     *
     * @return     the boolean value of the implementation property,
     *             or <code>def</code> if there is no property with that key or
     *             the config value is not a boolean.
     *
     * @exception  NullPointerException if <code>key</code> is
     *             <code>null</code>.
     * @exception  IllegalArgumentException if <code>key</code> is empty.
     */
    public static boolean getBoolProperty(String key, boolean def) {
        String value = System.getProperty(key);
        if (value != null) {
            try {
                return Boolean.valueOf(value).booleanValue();
            } catch (NumberFormatException e) {
            }
        }
        return def;
    }

    /**
     * Set the implementation property indicated by the specified key.
     *
     * @param      key   the name of the implementation property.
     * @param      val   the value of the implementation property.
     *
     * @exception  NullPointerException if <code>key</code> is
     *             <code>null</code>.
     * @exception  IllegalArgumentException if <code>key</code> is empty.
     */
    public static void setProperty(String key, String val) {
        System.setProperty(key, val);
    }

    /**
     * Gets all system properties names.
     *
     * @return  the string contains space separated system properties list
     */
    public static String getSystemPropertiesNames() {
        Properties props = AccessController.doPrivileged(new PrivilegedAction<Properties>() {
                public Properties run() {
                    return System.getProperties();
                }
            });
        String result = new String();
        try {
            Enumeration<?> names = props.propertyNames();
            while (names.hasMoreElements()) {
                result += (String)names.nextElement() + " ";
            }
        } catch (ClassCastException e) {
        }
        return result;
    }
}
