/*
 *
 * Copyright (c) 1990, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * Interface for UTF-16 string handling.
 * javacall_utf16_string type represents itself string of characters in
 * UTF-16 representation with terminating zero character
 *
 */

#ifndef _JAVAUTIL_UNICODE_H_
#define _JAVAUTIL_UNICODE_H_

#ifdef __cplusplus
extern "C" {
#endif

#include "javacall_defs.h"

#define COLON    0x3A    /* Colon ':' - Unicode character 0x3A */
#define CR       0x0D    /* Carriage Return - Unicode character 0x0D */
#define LF       0x0A    /* Line Feed - Unicode character 0x0A */
#define SPACE    0x20    /* Space - Unicode character 0x20 */
#define HTAB     0x09    /* Horizontal TAB - Unicode character 0x09 */
#define POUND    0x23    /* '#' - Unicode character 0x23 */

/**
 * Returns whether the specified integer value is a Unicode code point.
 */
#define IS_UNICODE_CODE_POINT(code_point) \
  ((code_point) >= 0x0 && (code_point) <= 0x10ffff)

/**
 * Returns whether the specified integer value is a BMP code point.
 */
#define IS_BMP_CODE_POINT(code_point) \
  ((code_point) >= 0x0 && (code_point) <= 0xffff)

/**
 * Returns whether the specified integer value is a surrogate code point.
 */
#define IS_SURROGATE_CODE_POINT(code_point) \
  ((code_point) >= 0xd800 && (code_point) <= 0xdfff)

/**
 * Converts the Unicode code point to UTF-16 code unit.
 * High surrogate is stored in code_unit[0],
 * low surrogate is stored in code_unit[1].
 *
 * @param code_point  Unicode code point
 * @param code_unit   Storage for UTF-16 code unit
 * @param unit_length Storage for the number of 16-bit units
 *                    in the UTF-16 code unit
 *
 * @return <code>JAVACALL_OK</code> on success
 *         <code>JAVACALL_FAIL</code> if code_unit or unit_length is NULL
 *         <code>JAVACALL_INVALID_ARGUMENT</code> invalid code point representation
 */
javacall_result javautil_unicode_codepoint_to_utf16codeunit(const javacall_int32 code_point,
                                                            /*OUT*/ javacall_utf16 code_unit[2],
                                                            /*OUT*/ javacall_int32* unit_length);

/**
 * Returns the number of 16-bit units in the UTF-16 representation
 * of the specified string, not including the terminating zero character.
 *
 * @param str string
 * @param length length of UTF-16 representation of the string
 *
 * @return <code>JAVACALL_OK</code> on success
 *         <code>JAVACALL_FAIL</code> if str or length is NULL
 */
javacall_result javautil_unicode_utf16_ulength(javacall_const_utf16_string str,
                                               /*OUT*/ javacall_int32* length);

/**
 * Returns the number of abstract characters in the UTF-16 representation
 * of the specified string, not including the terminating zero character.
 *
 * @param str string
 * @param length number of abstract characters in the UTF-16 representation
 *               of the string
 *
 * @return <code>JAVACALL_OK</code> on success
 *         <code>JAVACALL_FAIL</code> if str or length is NULL
 *         <code>JAVACALL_INVALID_ARGUMENT</code> invalid UTF-16 representation
 */
javacall_result javautil_unicode_utf16_chlength(javacall_const_utf16_string str,
                                                /*OUT*/ javacall_int32* length);

/**
 * Returns the number of bytes in the UTF-8 representation
 * of the specified string, not including the terminating zero character.
 *
 * @param str string
 * @param length number the number of bytes in the UTF-8 representation
 *               of the string
 *
 * @return <code>JAVACALL_OK</code> on success
 *         <code>JAVACALL_FAIL</code> if str or length is NULL
 *         <code>JAVACALL_INVALID_ARGUMENT</code> invalid UTF-16 representation
*/
javacall_result javautil_unicode_utf16_utf8length(javacall_const_utf16_string str,
                                                  /*OUT*/ javacall_int32* length);

/**
 * Converts the UTF-16 to UTF-8.
 * If length is not NULL, the number of bytes in the UTF-8 representation
 * of the string is written to it.
 * Note: no terminating zero character is added explicitly.
 * If buffer is NULL, the conversion is performed, but its result is
 * not written.
 * If buffer is not NULL and utf8Len length is not sufficient, the conversion is not
 * performed and the function returns JAVACALL_FAIL.
 *
 * @param pUTF16 UTF-16 input buffer
 * @param utf16Len number of 16-bit units in UTF-16 string representation
 * @param pUtf8 UTF-8 result buffer
 * @param utf8Len length of the UTF-8 result buffer
 * @param length storage for the number of bytes in UTF-8 representation
 *               of the string
 *
 * @return <code>JAVACALL_OK</code> on success
 *         <code>JAVACALL_FAIL</code> if the result buffer is to small
 *         <code>JAVACALL_INVALID_ARGUMENT</code>invalid UTF-16 representation
 */
javacall_result javautil_unicode_utf16_to_utf8(const javacall_utf16* pUtf16,
                                               javacall_int32 utf16Len,
                                               unsigned char* pUtf8,
                                               javacall_int32 utf8Len,
                                               /*OUT*/ javacall_int32* length);

/**
 * Calculates maximum number of UTF8 bytes in pUtf8 buffer that can be converted into UTF16 buffer of length *utf16Len.
 * Returns the number of consumed UTF8 bytes in *utf8Len and the number of required UTF16 chars in *utf16Len.
 *
 * @param pUtf8 UTF-8 bytes buffer (may contain not whole number of characters)
 * @param utf8Len in: the number of bytes in pUtf8; out: the number of bytes that can be converted
 * @param utf16Len in: upper bound for UTF-16 positions; out: the number of UTF-16 positions required for converting *utf8Len UTF-8 bytes.
 *
 * @return if pUtf8 string contains illegal UTF8 codes JAVACALL_INVALID_ARGUMENT otherwise JAVACALL_OK
 */
javacall_result javautil_unicode_get_max_lengths(const unsigned char * pUtf8,
                                                 /*IN/OUT*/javacall_int32 * utf8Len,
                                                 /*IN/OUT*/javacall_int32 * utf16Len);

/**
 * Converts the UTF-8 to UTF-16.
 * If length is not NULL, the number of 16-bit units in the UTF-16 representation
 * of the string is written to it.
 * Note: no terminating zero character is added explicitly.
 * If buffer is NULL, the conversion is performed, but its result is
 * not written.
 * If buffer is not NULL and utf16Len length is not sufficient, the conversion is not
 * performed and the function returns JAVACALL_FAIL.
 *
 * @param pUtf8 UTF-8 input buffer
 * @param utf8Len number of UTF-8 chars to convert
 * @param pUtf16 UTF-16 result buffer
 * @param utf16Len length of the UTF-16 result buffer
 * @param length storage for the number of 16-bit units in UTF-16 representation
 *               of the string
 *
 * @return <code>JAVACALL_OK</code> on success
 *         <code>JAVACALL_FAIL</code> if the result buffer is to small
 *         <code>JAVACALL_INVALID_ARGUMENT</code>invalid UTF-8 representation
 */
javacall_result javautil_unicode_utf8_to_utf16(const unsigned char* pUtf8,
                                               javacall_int32 utf8Len,
                                               javacall_utf16* pUtf16,
                                               javacall_int32 utf16Len,
                                               /*OUT*/ javacall_int32* length);

/**
 * Checks to see if two strings are equal in UTF-16 representation.
 * Strings are ended by the terminating zero character.
 *
 * @param str1 first string
 * @param str2 second string
 *
 * @return <code>JAVACALL_TRUE</code> if strings are not NULL and are equal,
 *         <code>JAVACALL_FALSE</code> otherwise.
 */
javacall_bool javautil_unicode_equals(javacall_const_utf16_string str1,
                                      javacall_const_utf16_string str2);
/**
 * Compares two strings in UTF-16 representation. The result of comparison
 * is an integer less than, equal to, or greater than zero if str1 is found,
 * respectively, to be less than, to match, or be greater than str2.
 *
 * @param str1 first string to compare
 * @param str2 second string to compare
 * @param comparison storage for the result of comparison
 *        < 0   str1 less than str2
 *        = 0   str1 identical to str2
 *        > 0   str1 greater than str2
 *
 * @return <code>JAVACALL_OK</code> on success,
 *         <code>JAVACALL_FAIL</code> one of the strings or result of comparison is NULL
 *         <code>JAVACALL_INVALID_ARGUMENT</code>invalid UTF-16 representation of
 *                                               one of the strings
 */
javacall_result javautil_unicode_cmp(javacall_const_utf16_string str1,
                                     javacall_const_utf16_string str2,
                                     /*OUT*/ javacall_int32* comparison);

/**
 * Compares first <number> characters of two strings in UTF-16 representation.
 * The result of comparison is an integer less than, equal to, or greater than zero if
 * str1 is found, respectively, to be less than, to match, or be greater than str2.
 *
 * @param str1 first string to compare
 * @param str2 second string to compare
 * @param number number of chars
 * @param comparison storage for the result of comparison
 *        < 0   str1 less than str2
 *        = 0   str1 identical to str2
 *        > 0   str1 greater than str2
 *
 * @return <code>JAVACALL_OK</code> on success,
 *         <code>JAVACALL_FAIL</code> one of the strings or result of comparison is NULL
 *         <code>JAVACALL_INVALID_ARGUMENT</code>invalid UTF-16 representation of
 *                                               one of the strings
 */
javacall_result javautil_unicode_ncmp(javacall_const_utf16_string str1,
                                      javacall_const_utf16_string str2,
                                      javacall_int32 number,
                                      /*OUT*/ javacall_int32* comparison);

/**
 * Compares two strings in UTF-16 representation without regard to case.
 * The result of comparison is an integer less than, equal to, or greater than zero if
 * str1 is found, respectively, to be less than, to match, or be greater than str2.
 *
 * @param str1 first string to compare
 * @param str2 second string to compare
 * @param comparison storage for the result of comparison
 *        < 0   str1 less than str2
 *        = 0   str1 identical to str2
 *        > 0   str1 greater than str2
 *
 * @return <code>JAVACALL_OK</code> on success,
 *         <code>JAVACALL_FAIL</code> one of the strings or result of comparison is NULL
 *         <code>JAVACALL_INVALID_ARGUMENT</code>invalid UTF-16 representation of
 *                                               one of the strings
 */
javacall_result javautil_unicode_icmp(javacall_const_utf16_string str1,
                                      javacall_const_utf16_string str2,
                                      /*OUT*/ javacall_int32* comparison);
/**
 * Compares first <number> characters of two strings in UTF-16 representation without regard
 * to case. The result of comparison is an integer less than, equal to, or greater than zero if
 * str1 is found, respectively, to be less than, to match, or be greater than str2.
 *
 * @param str1 first string to compare
 * @param str2 second string to compare
 * @param number number of chars
 * @param comparison storage for the result of comparison
 *        < 0   str1 less than str2
 *        = 0   str1 identical to str2
 *        > 0   str1 greater than str2
 *
 * @return <code>JAVACALL_OK</code> on success,
 *         <code>JAVACALL_FAIL</code> one of the strings or result of comparison is NULL
 *         <code>JAVACALL_INVALID_ARGUMENT</code>invalid UTF-16 representation of
 *                                               one of the strings
 */
javacall_result javautil_unicode_nicmp(javacall_const_utf16_string str1,
                                       javacall_const_utf16_string str2,
                                       javacall_int32 number,
                                       /*OUT*/ javacall_int32* comparison);

/**
 * Creates a new string that is a concatenation of the two specified strings.
 * The terminating zero character at the end of the first string is stripped
 * before the concatenation.
 *
 * @param str1   first string to concatenate
 * @param str2   second string to concatenate
 * @param dst    storage for the created concatenation
 * @param dstLen available <code>dst</code> storage space in
 *               <code>javacall_utf16</code> units
 *
 * @return <code>JAVACALL_OK</code> on success
 *         <code>JAVACALL_FAIL</code> if the result buffer is NULL
 *         <code>JAVACALL_OUT_OF_MEMORY</code> if not enough storage space
 */
javacall_result javautil_unicode_cat(javacall_const_utf16_string str1,
                                     javacall_const_utf16_string str2,
                                     /*OUT*/ javacall_utf16_string dst,
                                     javacall_int32 dstLen);

/**
 * Converts a given string representation of decimal integer to integer.
 *
 * @param str string representation of integer
 * @param number the integer value of str
 *
 * @return <code>JAVACALL_OK</code> on success,
 *         <code>JAVACALL_FAIL</code> if the number or str is NULL
 *         <code>JAVACALL_INVALID_ARGUMENT</code>if arithmetic overflow occures or
 *                                               not integer value in the string was
 *                                               detected
 */
javacall_result javautil_unicode_to_int32(javacall_const_utf16_string str,
                                          /*OUT*/ javacall_int32* number);

/**
 * Creates a new string representing the specified 32-bit signed integer value.
 * The integer value is converted to signed decimal representation.
 *
 * @param number the integer value of str
 * @param str    buffer for string representation of integer
 * @param strLen available buffer size in <code>javacall_utf16</code> units
 *
 * @return <code>JAVACALL_OK</code> on success,
 *         <code>JAVACALL_FAIL</code> if the str is NULL
 *         <code>JAVACALL_OUT_OF_MEMORY</code> buffer size is too small
 */
javacall_result javautil_unicode_from_int32(javacall_int32 number,
                                            /*OUT*/ javacall_utf16_string str,
                                            javacall_int32 strLen);

/**
 * Remove white spaces from the end of a string
 *
 * @param str string to trim
 * @return <code>JAVACALL_OK</code> on success
 *         <code>JAVACALL_FAIL</code>if str is NULL or has zero length
 */
javacall_result javautil_unicode_trim(javacall_utf16_string str);

/**
 * Returns a new string that is a substring of this string. The
 * substring begins at the specified <code>begin</code> and
 * extends to the character at index <code>end - 1</code>.
 * Thus the length of the substring is <code>end-begin</code>.
 * Note: Works with abstract character of the UTF-16 representation
 *
 * @param src input string
 * @param begin the beginning index, inclusive.
 * @param end the ending index, exclusive.
 * @param dest buffer to store the substring in
 * @param destLen available size of the buffer in <code>javacall_utf16</code>
 *                units
 *
 * @return <code>JAVACALL_OK</code> on success,
 *         <code>JAVACALL_FAIL</code> if the str or dest is NULL
 *         <code>JAVACALL_INVALID_ARGUMENT</code>invalid indexes, invalid UTF-16
 *                                               representation of the input string
 *         <code>JAVACALL_OUT_OF_MEMORY</code> if the buffer is too small
 */
javacall_result javautil_unicode_substring(javacall_const_utf16_string src,
                                           javacall_int32 begin,
                                           javacall_int32 end,
                                           /*OUT*/ javacall_utf16_string dest,
                                           javacall_int32 destLen);

/**
 * Looks for first occurrence of <param>ch</param> within <param>str</param>
 * Note: Result index is index of abstract character of the UTF-16 representation
 *
 * @param str string to search in
 * @param ch character to look for
 * @param index index of the first occurence of <param>c</param>
 *
 * @return <code>JAVACALL_OK</code> on success,
 *         <code>JAVACALL_FAIL</code> str or index is NULL or character does not exist
 *                                    in the string
 *         <code>JAVACALL_INVALID_ARGUMENT</code> invalid code point representation ,
 *                                                invalid UTF-16 representation of
 *                                                the input string
 */
javacall_result javautil_unicode_index_of(javacall_const_utf16_string str,
                                          javacall_int32 ch,
                                          /* OUT */ javacall_int32* index);
/**
 * Looks for the last occurence of <param>c</param> within <param>str</param>
 * Note: Result index is index of abstract character of the UTF-16 representation
 *
 * @param str string to search in
 * @param c character to look for
 * @param index index of the first occurence of <param>c</param>
 * @return <code>JAVACALL_OK</code> on success,
 *         <code>JAVACALL_FAIL</code> str or index is NULL or character does not exist
 *                                    in the string
 *         <code>JAVACALL_INVALID_ARGUMENT</code> invalid code point representation ,
 *                                                invalid UTF-16 representation of
 *                                                the input string
 */
javacall_result javautil_unicode_last_index_of(javacall_const_utf16_string str,
                                               javacall_int32 ch,
                                               /* OUT */ javacall_int32* index);

#ifdef __cplusplus
}
#endif

#endif /* _JAVAUTIL_UNICODE_H_ */
