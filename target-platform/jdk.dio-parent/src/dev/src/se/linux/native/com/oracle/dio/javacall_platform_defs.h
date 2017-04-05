/*
 * Copyright (c) 2008, 2012, Oracle and/or its affiliates. All rights reserved.
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

#ifndef __JAVACALL_PLATFORM_DEFINE_H_
#define __JAVACALL_PLATFORM_DEFINE_H_

#include <string.h>

/**
 * @file javacall_platform_defs.h
 * @ingroup Common
 * @brief Platform-dependent definitions for javacall
 */

/**
 * @defgroup Platform Platfrom specific Common Javacall API Definitions
 * @ingroup Common
 * The platform specific common javacall definitions are defined in this file
 * @{
 */

#ifdef __cplusplus
extern "C" {
#endif

#define JAVACALL_LOG_INFORMATION 0
#define JAVACALL_LOG_WARNING 1
#define JAVACALL_LOG_ERROR 2
#define JAVACALL_LOG_CRITICAL 3
#define JAVACALL_LOG_DISABLED 4

/**
 * @typedef javacall_utf16
 * @brief general unicode string type
 */
typedef unsigned short javacall_utf16;

/**
 * @typedef javacall_uint8
 * @brief 8 bit unsigned interger type
 */
typedef unsigned char javacall_uint8;

/**
 * @typedef javacall_int8
 * @brief 8 bit signed interger type
 */
typedef char javacall_int8;

/**
 * @typedef javacall_uint16
 * @brief 16 bit unsigned interger type
 */
typedef unsigned short javacall_uint16;

/**
 * @typedef javacall_uint32
 * @brief 32 bit unsigned interger type
 */
typedef unsigned long javacall_uint32;

/**
 * @typedef javacall_uint64
 * @brief 64 bit unsigned integer type
 */
typedef unsigned long long javacall_uint64;

/**
 * @typedef javacall_int16
 * @brief 16 bit signed interger type
 */
typedef signed short javacall_int16;

/**
 * @typedef javacall_int32
 * @brief 32 bit interger type
 */
typedef signed long javacall_int32;

/**
 * @typedef javacall_int64
 * @brief 64 bit interger type
 */
typedef long long javacall_int64;

/**
 * @def JAVACALL_MAX_HOST_LENGTH
 *
 */
#define JAVACALL_MAX_HOST_LENGTH (256)

/**
 * @def JAVACALL_MAX_URL_LENGTH
 *
 */
#define JAVACALL_MAX_URL_LENGTH 512

/**
 * @def JAVACALL_MAX_ACCESS_POINTS
 * Maximal number of access points defined on the device
 */
#define JAVACALL_MAX_ACCESS_POINTS         16

/**
 * @def JAVACALL_MAX_EVENT_SIZE
 * Maximal length of event data
 */
#define JAVACALL_MAX_EVENT_SIZE        512

/**
 * @def JAVACALL_MAX_FILE_NAME_LENGTH
 * Maximal length of file name supported
 */
#define JAVACALL_MAX_FILE_NAME_LENGTH         256

/**
 * @def JAVACALL_MAX_ILLEGAL_FILE_NAME_CHARS
 * Maximal number of illegal chars
 */
#define JAVACALL_MAX_ILLEGAL_FILE_NAME_CHARS  256

/**
 * @def JAVACALL_MAX_ROOTS_LIST_LENGTH
 * Maximal length of a list of file system roots
 */
#define JAVACALL_MAX_ROOTS_LIST_LENGTH  8192

/**
 * @def JAVACALL_MAX_ROOT_PATH_LENGTH
 * Maximal length of a file system root path
 */
#define JAVACALL_MAX_ROOT_PATH_LENGTH   256

/**
 * @def JAVACALL_MAX_LOCALIZED_ROOTS_LIST_LENGTH
 * Maximal length of a list of localized names of file system roots
 */
#define JAVACALL_MAX_LOCALIZED_ROOTS_LIST_LENGTH  8192

/**
 * @def JAVACALL_MAX_LOCALIZED_DIR_NAME_LENGTH
 * Maximal length of a localized name of a special directory
 */
#define JAVACALL_MAX_LOCALIZED_DIR_NAME_LENGTH    512

/**
 * @def JAVACALL_PIM_MAX_ARRAY_ELEMENTS
 *
 */
#define JAVACALL_PIM_MAX_ARRAY_ELEMENTS (10)
/**
 * @def JAVACALL_PIM_MAX_ATTRIBUTES
 *
 */
#define JAVACALL_PIM_MAX_ATTRIBUTES     (15)
/**
 * @def JAVACALL_PIM_MAX_FIELDS
 *
 */
#define JAVACALL_PIM_MAX_FIELDS         (19)


/**
 * @def JAVACALL_FONT_SIZE_SMALL
 *
 */
#define JAVACALL_FONT_SIZE_SMALL    8
/**
 * @def JAVACALL_FONT_SIZE_MEDIUM
 *
 */
#define JAVACALL_FONT_SIZE_MEDIUM   12
/**
 * @def JAVACALL_FONT_SIZE_LARGE
 *
 */
#define JAVACALL_FONT_SIZE_LARGE    16

/**
 * @def JAVACALL_INVALID_NETWORK_HANDLE
 * Defines platform specific invalid socket/datagram handle
 *
 * IMPL_NOTE: this value is reserved and should not used
 * as valid handle value
 */
#define JAVACALL_INVALID_NETWORK_HANDLE ((javacall_handle)-1)

/**
 * @def JAVACALL_PROPERTIES_FILENAME
 */
#define JAVACALL_PROPERTIES_FILENAME \
    {'j', 'w', 'c', '_', 'p', 'r', 'o', 'p', 'e', 'r', 't', 'i', 'e', 's',\
     '.', 'i', 'n', 'i', '\0'}

#define NOT_USED(var)                   ((void)var)

#ifdef ENABLE_DEVICEACCESS
/**
 * Maximum length of peripheral name
 */
#define JAVACALL_DEVICEACCESS_MAX_NAME_LEN 32
#endif


#define javautil_strlen strlen
#define javautil_strcpy strcpy
#define javautil_strcat strcat
#define javautil_strcmp strcmp
#define javautil_stricmp stricmp
#define javautil_memset memset
#define javautil_memcpy memcpy

#ifdef _DEBUG
#include <assert.h>
#define ASSERT(_t) assert(_t)
#else
#define ASSERT(_t)
#endif

/**
 * @}
 */

#ifdef __cplusplus
}
#endif

#endif


