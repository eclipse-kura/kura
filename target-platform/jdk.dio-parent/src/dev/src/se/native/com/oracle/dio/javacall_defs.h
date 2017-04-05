/**
 * @mainpage Javacall API
 * <h3>Reference Documentation for Javacall Porting API</h3>
 *
 * <p>These pages specify the Javacall porting APIs. They
 * describe the header files' contents, including function
 * signatures, globals, and data structures. The pages
 * organize the files both functionally by subsystem and
 * service, and alphabetically. They also index functions,
 * globals, and data structures for easier information access.
 * </p>
 *

 *
 */

/*
 * Copyright (c) 2006, 2010, Oracle and/or its affiliates. All rights reserved.
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

#ifndef __JAVACALL_DEFINE_H_
#define __JAVACALL_DEFINE_H_

/**
 * @file javacall_defs.h
 * @ingroup Common
 * @brief Common definitions for javacall
 */

/**
 * @defgroup Common Common Javacall API Definitions
 *
 * The common javacall definitions are defined in this file
 * @{
 */

#ifdef __cplusplus
extern "C" {
#endif

/*
 * To use javacall wrappers
 *   turn USE_JAVACALL_WRAPPERS on  for javacall files and
 *   turn USE_JAVACALL_WRAPPERS off for native test files.
 * See the document below for details:
 *   porting_api/tools/wrappers/README.txt
 */
#ifdef USE_JAVACALL_WRAPPERS
#include "javacall_wrappers_define.h"
#endif

/**
 * @define NULL
 * @brief A null pointer constant
 */
#ifndef NULL
#ifdef __cplusplus
#define NULL    0
#else
#define NULL    ((void*)0)
#endif
#endif

/**
 * @enum javacall_result
 * @brief javacall error results
 */
typedef enum {
   /** Generic success */
   JAVACALL_OK = 0,
   /** Generic failure */
   JAVACALL_FAIL = -1,
   /** Not implemented */
   JAVACALL_NOT_IMPLEMENTED = -2,
   /** Out of memory */
   JAVACALL_OUT_OF_MEMORY = -3,
   /** Invalid argument */
   JAVACALL_INVALID_ARGUMENT = -4,
   /** Would block */
   JAVACALL_WOULD_BLOCK = -5,
   /** Connection not found */
   JAVACALL_CONNECTION_NOT_FOUND = -6,
   /** Operation is interrupted */
   JAVACALL_INTERRUPTED = -7,
   /** Return by javacall read on
       SoS connections or socket in
       Non-Delay mode. Caller should
       reinvoke the read function
       to retry reading data */
   JAVACALL_NO_DATA_AVAILABLE = -8,
   /** File not found in the given path */
   JAVACALL_FILE_NOT_FOUND = -9,
   /** bad file name */
   JAVACALL_BAD_FILE_NAME = -10,
   /** End of file */
   JAVACALL_END_OF_FILE = -11,
   /** I/O error occurred */
   JAVACALL_IO_ERROR = -12,
   /** bad properties in jad file,
    * either a missing required property or
    * incorrectly formatted property */
   JAVACALL_BAD_JAD_PROPERTIES = -13,
    /** javacall properties db value not found */
   JAVACALL_VALUE_NOT_FOUND = -14,
    /** Invalid state */
   JAVACALL_INVALID_STATE = -15,
   /** Timeout elapsed */
   JAVACALL_TIMEOUT = -16,
   /** No audio device found. Return this code only in case you want to
     * reject playback, i.e. when the content is audio only. If some playback
     * is still possible (e.g. mute video) then return JAVACALL_OK instead
     */
   JAVACALL_NO_AUDIO_DEVICE = -17,
   /** Data supplied to javacall is unsuitable for some reason, corrupted, etc */
   JAVACALL_BAD_DATA_FORMAT = -18,
   /**
    * A JavaNotify event that makes the function call invalid (or unnecessary)
    * happened.
    *
    * For example, \ref JAVACALL_EVENT_MEDIA_DEVICE_UNAVAILABLE was sent (but
    * not yet received) just before \a Java called \ref javacall_media_play
    */
   JAVACALL_INVALID_SINCE_EVENT = -19,
   /** the processing initiated by the previous call of the function is still
     * not finished, so right now the implementation is busy and unable to
     * process the current function call.
     */
   JAVACALL_BUSY = -20,
   /**
    * Signals that an error occurred while attempting to bind a socket to a
    * local address and port.
    */
   JAVACALL_BIND_ERROR = -21,
   /**
    * Signals that an error occurred while attempting to connect a socket to a
    * remote address and port. Typically, the connection was refused remotely
    * (e.g., no process is listening on the remote address/port).
    */
   JAVACALL_CONNECT_ERROR = -22,
   /**
    * Signals that an error occurred while attempting to connect a socket to a
    * remote address and port. Typically, the remote host cannot be reached
    * because of an intervening firewall, or if an intermediate router is down.
    */
   JAVACALL_NO_ROUTE_TO_HOST_ERROR = -23,
    /**
     * Signals that an ICMP Port Unreachable message has been received on a
     * connected datagram.
     */
   JAVACALL_PORT_UNREACHABLE_ERROR = -24,

   /**
    * The handle is invalid.
    */
   JAVACALL_INVALID_HANDLE_ERROR = -25
} javacall_result;

/**
 * @define JAVACALL_SUCCEEDED
 * @param Status a status code to check
 * @brief true if the Status parameter corresponds to successful operation completion, false otherwise
 */
#define JAVACALL_SUCCEEDED(Status) ((javacall_result)(Status) >= 0)

/**
 * @enum javacall_bool
 * @brief javacall boolean type
 */
typedef enum {
    /** FALSE */
    JAVACALL_FALSE = 0,
    /** TRUE */
    JAVACALL_TRUE  = 1
} javacall_bool;

/**
 * @typedef javacall_handle
 * @brief general handle type
 */
typedef void* javacall_handle;

/**
 * Platform-dependent defines,
 * check JAVACALL_PLATFORM_INC_DIR environment variable
 */
#include <javacall_platform_defs.h>

/**
 * @define javacall_assert
 * @brief javacall_platform_defs.h can override this definition
 * @note MIDP, PCSL or JAVACALL must define ENABLE_DEBUG=1 for debug mode
 */
#ifndef javacall_assert
/*   #if ENABLE_DEBUG
    extern void javacall_print(const char *s);
    // the simplest & most compatible implementation
    #define javacall_assert(c) \
        (c) ? (void)0 : (\
            (javacall_print(#c ": ASSERT FAIL\n")), \
            (void)(*(int*)0x00000000 = 0))
    #else
*/
    #define javacall_assert(c) (void)0
/*    #endif */
#endif

/**
 * @typedef javacall_suite_id
 * @brief suite unique ID
 */
typedef javacall_int32 javacall_suite_id;

/**
 * @brief unique storage ID
 */
typedef javacall_int32 javacall_storage_id;

/**
 * @brief unique AMS folder ID
 */
typedef javacall_int32 javacall_folder_id;

/**
 * @brief unique running midlet ID
 */
typedef javacall_int32 javacall_app_id;

/**
 * @brief unique RMS ID
 */
typedef javacall_int32 javacall_rms_id;

/**
 * @brief unique RMS record ID
 */
typedef javacall_int32 javacall_record_id;

/**
 * @enum javacall_ip_version
 * @brief javacall IP version type
 */
typedef enum
{
   /**
   * Undefined Internet Protocol version
   */
   JAVACALL_IP_VERSION_ANY = 0,

   /**
    * The Internet Protocol version 4 (IPv4)
    */
   JAVACALL_IP_VERSION_4 = 2,

   /**
    * The Internet Protocol version 6 (IPv6)
    */
   JAVACALL_IP_VERSION_6 = 23,

 } javacall_ip_version;

/**
 * @define ADDR_LEN
 * @brief size in bytes of an IP address depending on the IP version
 */
#define ADDR_LEN(ip_version) (JAVACALL_IP_VERSION_4 == ip_version ? 4 : 16)

/**
 * @define JAVACALL_INVALID_SUITE_ID
 * @brief The suite_id that does not correspond to any midlet suite
 *
 * IMPL_NOTE: value -1 is reserved for internal (rommized) MIDlet suites
 */
#define JAVACALL_INVALID_SUITE_ID (-2)

/** Suite ID that is never used. (see the com.sun.midp.midlet.MIDletSuite class)*/
#define JAVACALL_UNUSED_SUITE_ID 0

/** Suite ID used for internal midlet suites. (see the com.sun.midp.midlet.MIDletSuite class)*/
#define JAVACALL_INTERNAL_SUITE_ID (-1)


/**
 * @define JAVACALL_INVALID_FOLDER_ID
 * @brief The folder id that does not correspond to any folder
 */
#define JAVACALL_INVALID_FOLDER_ID (-1)

/**
 * @define JAVACALL_ROOT_FOLDER_ID
 * @brief ID of the root folder
 */
#define JAVACALL_ROOT_FOLDER_ID (-2)

/**
 * @define JAVACALL_INVALID_STORAGE_ID
 * @brief The storage id that does not correspond to any storage
 */
#define JAVACALL_INVALID_STORAGE_ID (-1)

/**
 * @define JAVACALL_INVALID_APP_ID
 * @brief The application id that does not correspond to any running application
 */
#define JAVACALL_INVALID_APP_ID (-1)

/**
 * @define JAVACALL_INVALID_DISPLAY_DEVICE_ID
 * @brief The display device id that does not correspond to any display device
 */
#define JAVACALL_INVALID_DISPLAY_DEVICE_ID (-1)

/**
 * @typedef javacall_utf16_string
 * @brief general utf16 string type, this type is null terminated string
 */
typedef javacall_utf16* javacall_utf16_string;

/**
 * @typedef javacall_const_utf16_string
 * @brief general constant utf16 string type, this type is constant null
 * terminated string
 */
typedef const javacall_utf16* javacall_const_utf16_string;

/**
 * @typedef javacall_utf8_string
 * @brief general utf8 string type, this type is null terminated string
 */
typedef unsigned char* javacall_utf8_string;

/**
 * @typedef javacall_const_utf8_string
 * @brief general constant utf8 string type, this type is constant null
 * terminated string
 */
typedef const unsigned char* javacall_const_utf8_string;

/**
 * @typedef javacall_ascii_string
 * @brief general eight-bit ASCII string type,
 *        this type is null terminated string
 */
typedef char* javacall_ascii_string;

/**
 * @typedef javacall_const_ascii_string
 * @brief general constant eight-bit ASCII string type,
 *        this type is constant null terminated string
 */
typedef const char* javacall_const_ascii_string;

/**
 * @def JAVACALL_INVALID_HANDLE
 * Invalid handle
 */
#define JAVACALL_INVALID_HANDLE    (javacall_handle)-1

#if ENABLE_DYNAMIC_PIXEL_FORMAT
/**
 * @typedef javacall_pixel16
 * @brief 16-bit pixel type for LCD graphics
 */
typedef unsigned short javacall_pixel16;
/**
 * @typedef javacall_pixel32
 * @brief 32-bit pixel type for LCD graphics
 */
typedef unsigned int javacall_pixel32;
/**
 * @typedef javacall_pixel
 * @brief Default pixel type for LCD graphics
 */
typedef javacall_pixel32 javacall_pixel;
#elif ENABLE_32BITS_PIXEL_FORMAT
/**
 * @typedef javacall_pixel
 * @brief Default pixel type for LCD graphics
 */
typedef unsigned int javacall_pixel;
#else
/**
 * @typedef javacall_pixel
 * @brief Default pixel type for LCD graphics
 */
typedef unsigned short javacall_pixel;
/**
 * @typedef javacall_pixel16
 * @brief 16-bit pixel type for LCD graphics
 */
typedef unsigned short javacall_pixel16;
#endif

/**
 * @brief A list of properties that can be searched by a key
 *
 * IMPL_NOTE: should be moved to nams/javacall_ams_common.h
 */
typedef struct _javacall_ams_properties {
    /**
     * Number of properties, there are 2 Strings (key/value)
     * for each property.
     */
    int numberOfProperties;
    /**
     * A pointer to an array of properties. Keys and values are interleaved.
     */
    javacall_utf16_string* pStringArr;
} javacall_ams_properties;

/**
 * @define JAVACALL_UNKNOWN_LENGTH
 * @brief Corresponds to unknown length
 * @note  Be careful with bit-depth context with signed-unsigned conversion: <br>
 * May happen that (\c unsigned \c int)JAVACALL_UNKNOWN_LENGTH != (\c long \c long)JAVACALL_UNKNOWN_LENGTH
 */
#define JAVACALL_UNKNOWN_LENGTH (-1)

/**
 * @brief Provides a version of the product
 */
extern javacall_const_ascii_string javacall_product_version;

/**
 * @}
 */
#ifdef __cplusplus
} // extern "C"
#endif

/**
 * @defgroup IMPNG IMPNG API
 *
 * This document describes the requirements for implementing IMPNG
 * ( Information Module Profile - Next Generation, JSR 228). <br>
 *
 * The IMP-NG specification is based on the IMP specification (see JSR-195) and provides backward compatibility with IMP so that IMlets written for IMP can execute in IMP-NG environments.
 * IMP-NG is a strict subset of the Mobile Information Device Profile (MIDP), version 2.0, which is described in Mobile Information Device Profile, version 2.0 (JSR-118).<br><br>
 * The IMP-NG is designed to operate on top of the Connected, Limited Device Configuration (CLDC) which is described in Connected, Limited Device Configuration (JSR-30) (http://jcp.org/jsr/detail/30.jsp).
 * While the IMP-NG specification was designed assuming only CLDC 1.0 features, it will also work on top of CLDC 1.1 (JSR-139) (http://jcp.org/jsr/detail/139.jsp), and presumably any newer versions.
 * It is anticipated, though, that most IMP-NG implementations will be based on CLDC 1.0.<br>
 *
 * The specifications be found at: http://jcp.org/en/jsr/detail?id=228
 *
 * @{
 * @}
 */
#endif /* __JAVACALL_DEFINE_H_ */
