/*
 *
 * Copyright (c) 1990, 2013, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @file
 *
 * Interface for property set and get handling.
 *
 */

#ifndef _JAVACALL_PROPERTIES_H_
#define _JAVACALL_PROPERTIES_H_

#include "javacall_defs.h"

/**
 * @enum javacall_property_type
 * @brief types used in javacall_get_property javacall_set_property functions
 */
typedef enum {
    /** Application related property */
    JAVACALL_APPLICATION_PROPERTY,
    /** Internal property */
    JAVACALL_INTERNAL_PROPERTY,
    /** Not used */
    JAVACALL_NUM_OF_PROPERTIES
} javacall_property_type;


/**
 * Gets the value of the specified property in the specified
 * property set.
 *
 * @param key The key to search for
 * @param type The property type
 * @param result Where to put the result
 *
 * @return If found: <tt>JAVACALL_OK</tt>, otherwise
 *         <tt>JAVACALL_FAIL</tt>
 */
javacall_result javacall_get_property(const char* key,
                                      javacall_property_type type,
                                      char** result);

/**
 * Gets the value of the specified property in the specified
 * property set and parse it into integer.  If the property has not been found,
 * assign NULL to result.
 *
 * @param key The key to search for
 * @param type The property type
 * @param int_value integer value parsed from property_value
 *
 * @return If found and parsed into int: <tt>JAVACALL_OK</tt>, otherwise
 *         <tt>JAVACALL_FAIL</tt>
 */
javacall_result javacall_get_property_int(const char* key,
                                          javacall_property_type type,
                                          int *int_value);

/**
 * Sets a property value matching the key in the specified
 * property set.
 *
 * @param key The key to set
 * @param value The value to set <tt>key</tt> to
 * @param replace_if_exist The value to decide if it's needed to replace
 * the existing value corresponding to the key if already defined
 * @param type The property type
 *
 * @return Upon success <tt>JAVACALL_OK</tt>, otherwise
 *         <tt>JAVACALL_FAIL</tt>
 */
javacall_result javacall_set_property(const char* key, const char* value,
                                      int replace_if_exist, javacall_property_type type);

/**
 * Initializes the configuration subsystem.
 *
 * @return <tt>JAVACALL_OK</tt> for success, <tt>JAVACALL_FAIL</tt> otherwise
 */
javacall_result javacall_initialize_configurations(void);

/**
 * Finalize the configuration subsystem.
 */
void javacall_finalize_configurations(void);

#endif  /* _JAVACALL_PROPERTIES_H_ */
