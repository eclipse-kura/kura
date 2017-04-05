/*
 * Copyright (c) 2006, 2013, Oracle and/or its affiliates. All rights reserved.
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


#ifndef __JAVACALL_LOGGING_H_
#define __JAVACALL_LOGGING_H_

/**
 * @file javacall_logging.h
 * @ingroup MandatoryLogging
 * @brief Javacall interfaces for logging
 */

#include "javacall_defs.h"

#ifdef __cplusplus
extern "C" {
#endif

/** @defgroup MandatoryLogging Logging API
 *  @ingroup IMPNG
 *
 *  @{
 */

/**
 * Prints out a string to a system specific output strream
 *
 * @param s a NULL terminated character buffer to be printed
 */
void javacall_print(const char *s);

/**
 * Prints out a character array to a system specific output strream
 *
 * @param s address of the first character to print
 * @param length number of the characters to print
 */
void javacall_print_chars(const char* s, int length);

#ifdef ENABLE_SYSTEM_ERR_STREAM
/**
 * Prints out a character array to the error output stream
 *
 * @param s address of the first character to print
 * @param length number of the characters to print
 */
void javacall_print_error_chars(const char* s, int length);
#endif

/**
 * Prints out a string to a system specific output strream
 * with format string.
 *
 * @param format format string and variable lists.
 */
/*OPTIONAL*/
void javacall_printf(const char * format,...);

#if ENABLE_TCP_LOGGING
typedef enum {
    no_implicit_cr_in_lf = 0,
    implicit_cr_in_lf = 1
} javacall_lf_processing;

/**
 * Put a character array to the TCP logging buffer
 *
 * @param data address of the first character to print
 * @param len number of the characters to print
 * @param lf_processing specifies line feed character (LF) processing
 */
void javanotify_tcp_logging(const char *data, int len, javacall_lf_processing lf_processing);
#endif /* ENABLE_TCP_LOGGING */

/**
 * Must be called before calling javanotify_log_string() and javanotify_log_char()
 * functions. Could lock some resources if some routines called in javanotify_log_string() or
 * javanotify_log_char() are not thread-safe.
 */
void javanotify_log_lock(void);

/**
 * Forward log string to all enabled log sinks (e.g. VM agent log channel, javacall_print,
 * TCP log).
 *
 * @param data pointer to null-terminated UTF8 string
 */
void javanotify_log_string(const char *data);

/**
 * Forward log char to all enabled log sinks (e.g. VM agent log channel, javacall_print,
 * TCP log).
 *
 * @param data character to print
 */
void javanotify_log_char(char data);

/**
 * The methods must be called to release common logging resources.
 * Each javanotify_log_lock(0 call must have corresponding javanotify_log_unlock() call.
 */
void javanotify_log_unlock(void);


#if ENABLE_VM_LOGGING
/**
 * Prints out a VM trace string to output stream provided by SDK or
 * by any other debugging tools available in development environment.
 * VM trace can contain information about GC, classes loading, exceptions,
 * method invocations, etc.
 *
 * @param task_id ID of VM task that originated trace message
 * @param msg string with VM trace message
 */
/*OPTIONAL*/
void javacall_logging_vm_trace(int task_id, const char *msg);
/**
 * Sends log messages accumulated in a javacall buffer to ODT agent.
 *
 * @param buffer string with VM trace messages
 */
/*OPTIONAL*/
void javanotify_logging_event(char *buffer);
#endif


/**
 * @enum midp_log_channels
 * @brief list of possible logging channels
 */
typedef enum {
    /** Javacall default channel */
    javacall_channel_default = 2608,
    /** Javacall system out */
    javacall_channel_system_out,
    /** Javacall JVM internal*/
    javacall_channel_jvm_internal,
    /** Javacall trace in */
    javacall_channel_trace_in,
    /** Javacall trace out */
    javacall_channel_trace_out
} midp_log_channels;

/**
 * Returns the jlong-specifier prefix used with type characters in
 * printf functions or wprintf functions to specify interpretation
 * of jlong, the 64-bit signed integer type,
 * e.g. for win32 is "%I64d", for linux is "%lld"
 */
const char * javacall_jlong_format_specifier(void);

/**
 * Returns the julong-specifier prefix used with type characters in
 * printf functions or wprintf functions to specify interpretation
 * of julong, the 64-bit unsigned integer type,
 * e.g. for win32 is "%I64u", for linux is "%llu"
 */
const char * javacall_julong_format_specifier(void);

/** @} */

/**
 * JavaCall logging severity levels, these values are assumed to be equal
 * to similar MIDP logging definitions.
 */

#define JAVACALL_LOGGING_INFORMATION 0
#define JAVACALL_LOGGING_WARNING  1
#define JAVACALL_LOGGING_ERROR  2
#define JAVACALL_LOGGING_CRITICAL  3
#define JAVACALL_LOGGING_DISABLED  4

/**
 * @enum javacall_logging_channel
 * @brief JavaCall logging channels
 *
 */
typedef enum {
    JC_NONE = 0,
    JC_TIME,
    JC_FILE,
    JC_WMA,
    JC_MMA,
    JC_PIM,
    JC_FC,
    JC_AMMS,
    JC_CHAPI,
    JC_NAMS,
    JC_BT,
    JC_ODT,
    JC_FONT,
    JC_LCD,
    JC_SOCKET,
    JC_MMS,
    JC_MAINLOOP,
    JC_PERFORMANCE,
    JC_LIFECYCLE,
    JC_EVENTS,
    JC_MEMORY,
    JC_SPRINT_EXT,
    JC_SENSOR,
    JC_CMS,
    JC_LOCATION,
    JC_SECURITY,
    JC_SERIAL,
    JC_MOBILE,
    JC_SIMCARD,
    JC_SYSTEMEVENT,
    JC_OJSB,
    JC_CONTACTLESS,
    JC_DAAPI,
    JC_DIO = JC_DAAPI,
    JC_PROXY,
    JC_NETWORK,
    JC_LINEUI,
    JC_MAX

} javacall_logging_channel;

#ifndef JAVACALL_REPORT_LEVEL
/** If report level is not defined, set it to information level */
#define JAVACALL_REPORT_LEVEL JAVACALL_LOGGING_INFORMATION
#endif


/**
 * Initializes Javacall logging subsytem.
 */
void javacall_logging_initialize(void);

/**
 * Report a message to the Logging service.
 *
 * The <code>message</code> parameter is treated as a format
 * string to the standard C library call printf would be, with
 * conversion specifications (%s, %d, %c, etc) causing the
 * conversion and output of each successive argument after
 * <code>message</code>  As with printf, having a conversion
 * character in <code>message</code> without an associated argument
 * following it is an error.
 *
 * To ensure that no character in <code>message</code> is
 * interpreted as requiring conversion, a safe way to call
 * this method is:
 * <code> javacall_logging_printf(severity, chanID, "%s", message); </code>

 * @param severity severity level of report
 * @param channelID area report relates to
 * @param filename source file name
 * @param lineno line number
 * @param format detail message to go with the report
 *                should not be NULL
 */
void javacall_logging_printf(int severity, javacall_logging_channel channelID,
        const char* filename, int lineno, const char *format, ...);

/**
 * Report a string in utf16 after given prefix message to Logging server.
 *
 * @param severity severity level of report
 * @param channelID area report relates to
 * @param filename source file name
 * @param lineno line number
 * @param prefix_ascii_msg a prefix message to print before the following utf16 message.
 * @param utf16_msg message in utf16 to print.
 * @param utf16_len length of utf16 message
 *                  if -1, utf16_msg is printed until NULL termination.
 */
/*OPTIONAL*/
void javacall_logging_utf16_print(int severity,
                                  javacall_logging_channel channelID,
                                  const char* filename,
                                  int lineno,
                                  const char *prefix,
                                  const javacall_utf16* msg,
                                  int msg_length);

/**
 * @name JAVACALL_REPORT_INFO*() macros
 * JAVACALL_REPORT_INFO*() macros are defined if <code>JAVACALL_REPORT_LEVEL
 * <= JAVACALL_LOGGING_INFORMATION</code>, and are empty otherwise.
 * @see javacall_logging_printf
 * @{
 */
#if JAVACALL_REPORT_LEVEL <= JAVACALL_LOGGING_INFORMATION
#define JAVACALL_REPORT_INFO(ch, msg) javacall_logging_printf(JAVACALL_LOGGING_INFORMATION, ch, __FILE__, __LINE__, msg)
#define JAVACALL_REPORT_INFO1(ch, msg, a1) javacall_logging_printf(JAVACALL_LOGGING_INFORMATION, ch, __FILE__, __LINE__, msg, a1)
#define JAVACALL_REPORT_INFO2(ch, msg, a1, a2) \
  javacall_logging_printf(JAVACALL_LOGGING_INFORMATION, ch, __FILE__, __LINE__, msg, a1, a2)
#define JAVACALL_REPORT_INFO3(ch, msg, a1, a2, a3) \
  javacall_logging_printf(JAVACALL_LOGGING_INFORMATION, ch, __FILE__, __LINE__, msg, a1, a2, a3)
#define JAVACALL_REPORT_INFO4(ch, msg, a1, a2, a3, a4) \
  javacall_logging_printf(JAVACALL_LOGGING_INFORMATION, ch, __FILE__, __LINE__, msg, a1, a2, a3, a4)
#define JAVACALL_REPORT_INFO5(ch, msg, a1, a2, a3, a4, a5) \
  javacall_logging_printf(JAVACALL_LOGGING_INFORMATION, ch, __FILE__, __LINE__, msg, a1, a2, a3, a4, a5)
#define JAVACALL_REPORT_INFO6(ch, msg, a1, a2, a3, a4, a5, a6) \
  javacall_logging_printf(JAVACALL_LOGGING_INFORMATION, ch, __FILE__, __LINE__, msg, a1, a2, a3, a4, a5, a6)
#define JAVACALL_REPORT_INFO7(ch, msg, a1, a2, a3, a4, a5, a6, a7) \
  javacall_logging_printf(JAVACALL_LOGGING_INFORMATION, ch, __FILE__, __LINE__, msg, a1, a2, a3, a4, a5, a6, a7)
#define JAVACALL_REPORT_INFO8(ch, msg, a1, a2, a3, a4, a5, a6, a7, a8) \
  javacall_logging_printf(JAVACALL_LOGGING_INFORMATION, ch, __FILE__, __LINE__, msg, a1, a2, a3, a4, a5, a6, a7, a8)

#define JAVACALL_REPORT_UTF16_INFO(ch, prefix, msg, len) \
    javacall_logging_utf16_print(JAVACALL_LOGGING_INFORMATION, ch, __FILE__, __LINE__, prefix, msg, len)

#else
#define JAVACALL_REPORT_INFO(ch, msg)
#define JAVACALL_REPORT_INFO1(ch, msg, a1)
#define JAVACALL_REPORT_INFO2(ch, msg, a1, a2)
#define JAVACALL_REPORT_INFO3(ch, msg, a1, a2, a3)
#define JAVACALL_REPORT_INFO4(ch, msg, a1, a2, a3, a4)
#define JAVACALL_REPORT_INFO5(ch, msg, a1, a2, a3, a4, a5)
#define JAVACALL_REPORT_INFO6(ch, msg, a1, a2, a3, a4, a5, a6)
#define JAVACALL_REPORT_INFO7(ch, msg, a1, a2, a3, a4, a5, a6, a7)
#define JAVACALL_REPORT_INFO8(ch, msg, a1, a2, a3, a4, a5, a6, a7, a8)

#define JAVACALL_REPORT_UTF16_INFO(ch, prefix, msg, len)
#endif
/** @} */

/**
 * @name JAVACALL_REPORT_WARN*() macros
 * JAVACALL_REPORT_WARN*() macros are defined if <code> JAVACALL_REPORT_LEVEL
 * <= JAVACALL_LOGGING_WARNING</code> and are empty otherwise.
 * @see javacall_logging_printf
 * @{
 */
#if JAVACALL_REPORT_LEVEL <= JAVACALL_LOGGING_WARNING
#define JAVACALL_REPORT_WARN(ch, msg) javacall_logging_printf(JAVACALL_LOGGING_WARNING, ch, __FILE__, __LINE__, msg)
#define JAVACALL_REPORT_WARN1(ch, msg, a1) javacall_logging_printf(JAVACALL_LOGGING_WARNING, ch, __FILE__, __LINE__, msg, a1)
#define JAVACALL_REPORT_WARN2(ch, msg, a1, a2) javacall_logging_printf(JAVACALL_LOGGING_WARNING, ch, __FILE__, __LINE__, msg, a1, a2)
#define JAVACALL_REPORT_WARN3(ch, msg, a1, a2, a3) \
  javacall_logging_printf(JAVACALL_LOGGING_WARNING, ch, __FILE__, __LINE__, msg, a1, a2, a3)
#define JAVACALL_REPORT_WARN4(ch, msg, a1, a2, a3, a4) \
  javacall_logging_printf(JAVACALL_LOGGING_WARNING, ch, __FILE__, __LINE__, msg, a1, a2, a3, a4)

#define JAVACALL_REPORT_UTF16_WARN(ch, prefix, msg, len) \
    javacall_logging_utf16_print(JAVACALL_LOGGING_WARNING, ch, __FILE__, __LINE__, prefix, msg, len)
#else
#define JAVACALL_REPORT_WARN(ch, msg)
#define JAVACALL_REPORT_WARN1(ch, msg, a1)
#define JAVACALL_REPORT_WARN2(ch, msg, a1, a2)
#define JAVACALL_REPORT_WARN3(ch, msg, a1, a2, a3)
#define JAVACALL_REPORT_WARN4(ch, msg, a1, a2, a3, a4)

#define JAVACALL_REPORT_UTF16_WARN(ch, prefix, msg, len)
#endif
/** @} */

/**
 * @name JAVACALL_REPORT_ERROR*() macros
 * JAVACALL_REPORT_ERROR*() macros are defined if <code> JAVACALL_REPORT_LEVEL
 * <= JAVACALL_LOGGING_ERROR</code> and are empty otherwise.
 * @see javacall_logging_printf
 * @{
 */
#if JAVACALL_REPORT_LEVEL <= JAVACALL_LOGGING_ERROR
#define JAVACALL_REPORT_ERROR(ch, msg) javacall_logging_printf(JAVACALL_LOGGING_ERROR, ch, __FILE__, __LINE__, msg)
#define JAVACALL_REPORT_ERROR1(ch, msg, a1) javacall_logging_printf(JAVACALL_LOGGING_ERROR, ch, __FILE__, __LINE__, msg, a1)
#define JAVACALL_REPORT_ERROR2(ch, msg, a1, a2) \
  javacall_logging_printf(JAVACALL_LOGGING_ERROR, ch, __FILE__, __LINE__, msg, a1, a2)
#define JAVACALL_REPORT_ERROR3(ch, msg, a1, a2, a3) \
  javacall_logging_printf(JAVACALL_LOGGING_ERROR, ch, __FILE__, __LINE__, msg, a1, a2, a3)
#define JAVACALL_REPORT_ERROR4(ch, msg, a1, a2, a3, a4) \
  javacall_logging_printf(JAVACALL_LOGGING_ERROR, ch, __FILE__, __LINE__, msg, a1, a2, a3, a4)
#define JAVACALL_REPORT_ERROR5(ch, msg, a1, a2, a3, a4, a5) \
  javacall_logging_printf(JAVACALL_LOGGING_ERROR, ch, __FILE__, __LINE__, msg, a1, a2, a3, a4, a5)
#define JAVACALL_REPORT_ERROR6(ch, msg, a1, a2, a3, a4, a5, a6) \
  javacall_logging_printf(JAVACALL_LOGGING_ERROR, ch, __FILE__, __LINE__, msg, a1, a2, a3, a4, a5, a6)

#define JAVACALL_REPORT_UTF16_ERROR(ch, prefix, msg, len) \
    javacall_logging_utf16_print(JAVACALL_LOGGING_ERROR, ch, __FILE__, __LINE__, prefix, msg, len)
#else
#define JAVACALL_REPORT_ERROR(ch, msg)
#define JAVACALL_REPORT_ERROR1(ch, msg, a1)
#define JAVACALL_REPORT_ERROR2(ch, msg, a1, a2)
#define JAVACALL_REPORT_ERROR3(ch, msg, a1, a2, a3)
#define JAVACALL_REPORT_ERROR4(ch, msg, a1, a2, a3, a4)
#define JAVACALL_REPORT_ERROR5(ch, msg, a1, a2, a3, a4, a5)
#define JAVACALL_REPORT_ERROR6(ch, msg, a1, a2, a3, a4, a5, a6)

#define JAVACALL_REPORT_UTF16_ERROR(ch, prefix, msg, len)
#endif
/** @} */

/**
 * @name JAVACALL_REPORT_CRIT*() macros
 * JAVACALL_REPORT_CRIT*() macros are defined if <code> JAVACALL_REPORT_LEVEL
 * <= JAVACALL_LOGGING_CRITICAL</code> and are empty otherwise.
 * @see javacall_logging_printf
 * @{
 */
#if JAVACALL_REPORT_LEVEL <= JAVACALL_LOGGING_CRITICAL
#define JAVACALL_REPORT_CRIT(ch, msg) javacall_logging_printf(JAVACALL_LOGGING_CRITICAL, ch, __FILE__, __LINE__, msg)
#define JAVACALL_REPORT_CRIT1(ch, msg, a1) javacall_logging_printf(JAVACALL_LOGGING_CRITICAL, ch,__FILE__, __LINE__, msg, a1)
#define JAVACALL_REPORT_CRIT2(ch, msg, a1, a2) \
  javacall_logging_printf(JAVACALL_LOGGING_CRITICAL, ch, __FILE__, __LINE__, msg, a1, a2)
#define JAVACALL_REPORT_CRIT3(ch, msg, a1, a2, a3) \
  javacall_logging_printf(JAVACALL_LOGGING_CRITICAL, ch, __FILE__, __LINE__, msg, a1, a2, a3)
#define JAVACALL_REPORT_CRIT4(ch, msg, a1, a2, a3, a4) \
  javacall_logging_printf(JAVACALL_LOGGING_CRITICAL, ch, __FILE__, __LINE__, msg, a1, a2, a3, a4)
#define JAVACALL_REPORT_CRIT5(ch, msg, a1, a2, a3, a4, a5) \
  javacall_logging_printf(JAVACALL_LOGGING_CRITICAL, ch, __FILE__, __LINE__, msg, a1, a2, a3, a4, a5)

#define JAVACALL_REPORT_UTF16_CRIT(ch, prefix, msg, len) \
    javacall_logging_utf16_print(JAVACALL_LOGGING_CRITICAL, ch, __FILE__, __LINE__, prefix, msg, len)
#else
#define JAVACALL_REPORT_CRIT(ch, msg)
#define JAVACALL_REPORT_CRIT1(ch, msg, a1)
#define JAVACALL_REPORT_CRIT2(ch, msg, a1, a2)
#define JAVACALL_REPORT_CRIT3(ch, msg, a1, a2, a3)
#define JAVACALL_REPORT_CRIT4(ch, msg, a1, a2, a3, a4)
#define JAVACALL_REPORT_CRIT5(ch, msg, a1, a2, a3, a4, a5)

#define JAVACALL_REPORT_UTF16_CRIT(ch, prefix, msg, len)
#endif
/** @} */

#if ENABLE_EMERGENCY_LOGGING
/**
 * Prints out all information on emergency status.
 *
 */
void javacall_logging_flash(void);
#endif /* ENABLE_EMERGENCY_LOGGING */

#ifdef __cplusplus
}
#endif

#endif
