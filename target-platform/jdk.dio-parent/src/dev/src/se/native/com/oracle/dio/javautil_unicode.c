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

#include <string.h>
#include "javautil_unicode.h"

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
                                                            /*OUT*/ javacall_int32* unit_length) {
    if(unit_length == NULL || code_unit == NULL) {
        return JAVACALL_FAIL;
    }

    if (IS_BMP_CODE_POINT(code_point)) {
        if (IS_SURROGATE_CODE_POINT(code_point)) {
            return JAVACALL_INVALID_ARGUMENT;
        }

        /* handle most cases here (ch is a BMP code point) */
        *unit_length = 1;
        code_unit[0] = (javacall_utf16)(code_point & 0xffff);
        return JAVACALL_OK;
    }
    else{
        if (IS_UNICODE_CODE_POINT(code_point)) {
            const javacall_int32 offset = code_point - 0x10000;
            code_unit[0] = (javacall_utf16)((offset >> 10) + 0xd800);
            code_unit[1] = (javacall_utf16)((offset & 0x3ff) + 0xdc00);
            *unit_length = 2;
            return JAVACALL_OK;
        }
        else {
            return JAVACALL_INVALID_ARGUMENT;
        }
    }
}

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
                                               /*OUT*/ javacall_int32* length) {
    javacall_const_utf16_string ptr;

    if (str == NULL || length == NULL) {
        return JAVACALL_FAIL;
    }

    for(ptr = str; *ptr != 0; ptr++) {
    }

    *length = (javacall_int32)(ptr-str);
    return JAVACALL_OK;
}

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
                                                /*OUT*/ javacall_int32* length) {
    javacall_utf16 input_char = 0;
    javacall_int32 len;

    if (str == NULL || length == NULL) {
        return JAVACALL_FAIL;
    }

    for(len = 0; *str != 0; len++) {
        input_char = *str++;
        if (input_char >= 0xd800 && input_char <= 0xdfff) {
            if (input_char > 0xdbff) {
                return JAVACALL_INVALID_ARGUMENT;
            }

            /* this is <high-half zone code> in UTF-16 */
            /* check next char is valid <low-half zone code> */
            {
                javacall_utf16 low_char = *str++;
                if (low_char < 0xdc00 || low_char > 0xdfff) {
                    return JAVACALL_INVALID_ARGUMENT;
                }
            }
        }
    }

    *length = len;
    return JAVACALL_OK;
}

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
                                                  /*OUT*/ javacall_int32* length) {
    javacall_int32 utf16Len;

    if (str == NULL || length == NULL) {
        return JAVACALL_FAIL;
    }

    javautil_unicode_utf16_ulength(str, &utf16Len);
    if (javautil_unicode_utf16_to_utf8(str, utf16Len, NULL, 0, length) != JAVACALL_OK) {
        return JAVACALL_INVALID_ARGUMENT;
    }

    return JAVACALL_OK;
}

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
                                               /*OUT*/ javacall_int32* length) {
    javacall_int32 i;
    javacall_int32 count;
    javacall_int32 len = 0;
    javacall_utf16 current;
    unsigned char output[4] = { 0 };

    for (i = 0; i < utf16Len; i++) {
        current = pUtf16[i];
        if (current < 0x80) {
            /* binary 0xxxxxxx where x is a char bit */
            output[0] = (unsigned char)current;
            count = 1;
        }
        else{
            if (current < 0x800) {
                /* binary 110xxxxx 10xxxxxx where x is a char bit */
                output[0] = (unsigned char)(0xC0 | ((current >> 6) & 0x1F));
                output[1] = (unsigned char)(0x80 | (current & 0x3F));
                count = 2;
            }
            else{
                /* Part for UTF-16, not for UCS-2 */
                if (current >= 0xd800 && current <= 0xdfff) {
                    if (current > 0xdbff) {
                        return JAVACALL_INVALID_ARGUMENT;
                    }

                    /* this is <high-half zone code> in UTF-16 */
                    {
                        javacall_utf16 low_char;
                        if (++i == utf16Len) {
                            return JAVACALL_INVALID_ARGUMENT;
                        }
                        low_char = pUtf16[i];

                        /* check next char is valid <low-half zone code> */
                        if (low_char < 0xdc00 || low_char > 0xdfff) {
                            return JAVACALL_INVALID_ARGUMENT;
                        }

                        {
                            javacall_int32 ucs4 = (current - 0xd800) * 0x400 + (low_char - 0xdc00) + 0x10000;

                            /* binary 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx where x is a char bit */
                            output[0] = (unsigned char)(0xF0 | ((ucs4 >> 18)) & 0x07);
                            output[1] = (unsigned char)(0x80 | ((ucs4 >> 12) & 0x3F));
                            output[2] = (unsigned char)(0x80 | ((ucs4 >> 6) & 0x3F));
                            output[3] = (unsigned char)(0x80 | (ucs4 & 0x3F));
                            count = 4;
                        }
                    }
                }
                else{
                    /* binary 1110xxxx 10xxxxxx 10xxxxxx where x is a char bit */
                    output[0] = (unsigned char)(0xE0 | ((current >> 12) & 0x0F));
                    output[1] = (unsigned char)(0x80 | ((current >> 6) & 0x3F));
                    output[2] = (unsigned char)(0x80 | (current & 0x3F));
                    count = 3;
                }
            }
        }

        if (pUtf8 != NULL) {
            if (len + count > utf8Len) {
                /* Can't continue, result buffer too small */
                return JAVACALL_FAIL;
            } else {
                int j;
                for (j = 0; j < count; j++) {
                    pUtf8[len+j] = output[j];
                }
            }
        }

        len += count;
    }

    if (length != NULL) {
       *length = len;
    }

    return JAVACALL_OK;
}

/**
 * Calculates maximum number of UTF8 bytes in pUtf8 buffer that can be converted into UTF16 buffer of length *utf16Len.
 * Returns the number of consumed UTF8 bytes in *utf8Len and the number of required UTF16 chars in *utf16Len.
 *
 * if pUtf8 string contains illegal UTF8 codes the function returns JAVACALL_INVALID_ARGUMENT
 * otherwise JAVACALL_OK
 */
javacall_result javautil_unicode_get_max_lengths(const unsigned char * pUtf8, javacall_int32 * utf8Len, javacall_int32 * utf16Len)
{
    javacall_int32 utf8idx = 0, utf16idx = 0;
    unsigned char byte;
    int utf8count = 0, utf16count;
    while( utf8idx < *utf8Len ) {
        byte = *pUtf8++;
        if( (byte & 0x80) == 0 ) utf8count = 1;
        else if( (byte & 0xE0) == 0xC0 ) utf8count = 2;
        else if( (byte & 0xF0) == 0xE0 ) utf8count = 3;
        else if( (byte & 0xF8) == 0xF0 ) utf8count = 4;
        utf16count = (utf8count + 1) >> 1;
        if( utf8idx + utf8count > *utf8Len || utf16idx + utf16count > *utf16Len )
            break;

        utf8idx += utf8count;
        utf16idx += utf16count;
        // check highest bits
        if( utf8count > 1 ){
            --utf8count;
            while( --utf8count ){
                byte = *pUtf8++;
                if( (byte & 0x80) != 0x80 )
                    return( JAVACALL_INVALID_ARGUMENT );
            }
            byte = *pUtf8++;
            if( (byte & 0x80) != 0 )
                return( JAVACALL_INVALID_ARGUMENT );
        }
    }
    *utf8Len = utf8idx;
    *utf16Len = utf16idx;
    return( JAVACALL_OK );
}

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
                                               /*OUT*/ javacall_int32* length) {
    javacall_int32 count;
    javacall_int32 len = 0;
    javacall_int32 j;
    javacall_utf16 output[2] = { 0 };
    unsigned char current, byte2, byte3, byte4;

    for (j = 0; j < utf8Len; ) {
        current = pUtf8[j++];

        if (current < 0x80) {
            /* binary 0xxxxxxx where x is a char bit */
            output[0] = (javacall_utf16)current;
            count = 1;
        }
        else{
            if ((current & 0xE0) == 0xC0) {
                /* binary 110xxxxx 10xxxxxx where x is a char bit */
                if (j >= utf8Len) {
                    /* Can't continue, input buffer too small */
                    return JAVACALL_INVALID_ARGUMENT;
                }

                byte2 = pUtf8[j++];
                if ((byte2 & 0xC0) != 0x80) {
                    return JAVACALL_INVALID_ARGUMENT;
                }

                output[0] = (javacall_utf16)(((current & 0x1F) << 6) |
                                             (byte2 & 0x3F));
                count = 1;
            }
            else{
                if ((current & 0xF0) == 0xE0) {
                    /* binary 1110xxxx 10xxxxxx 10xxxxxx where x is a char bit */
                    if ((j+1) >= utf8Len) {
                        /* Can't continue, input buffer too small */
                        return JAVACALL_INVALID_ARGUMENT;
                    }

                    byte2 = pUtf8[j++];
                    byte3 = pUtf8[j++];
                    if (((byte2 & 0xC0) != 0x80) ||
                        ((byte3 & 0xC0) != 0x80)) {
                        return JAVACALL_INVALID_ARGUMENT;
                    }

                    output[0] = (javacall_utf16)(((current & 0x0F) << 12)|
                                                 ((byte2 & 0x3F) << 6)|
                                                   (byte3 & 0x3F));
                    count = 1;
                }
                else{
                    /* Part for UTF-16, not for UCS-2 */
                    if ((current & 0xF8) == 0xF0) {
                        /* binary 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx where x is a char bit */
                        if ((j+2) >= utf8Len) {
                            /* Can't continue, input buffer too small */
                            return JAVACALL_INVALID_ARGUMENT;
                        }

                        byte2 = pUtf8[j++];
                        byte3 = pUtf8[j++];
                        byte4 = pUtf8[j++];
                        if (((byte2 & 0xC0) != 0x80) ||
                            ((byte3 & 0xC0) != 0x80) ||
                            ((byte4 & 0xC0) != 0x80)) {
                            return JAVACALL_INVALID_ARGUMENT;
                        }

                        {
                            /* this byte sequence is UTF16 character */
                            javacall_int32 ucs4 = (javacall_int32)(0x07 & current) << 18 |
                                                  (javacall_int32)(0x3F & byte2) << 12 |
                                                  (javacall_int32)(0x3F & byte3) <<  6 |
                                                  (javacall_int32)(0x3F & byte4);

                            output[0] = (javacall_utf16)((ucs4 - 0x10000) / 0x400 + 0xd800);
                            output[1] = (javacall_utf16)((ucs4 - 0x10000) % 0x400 + 0xdc00);
                            count = 2;
                        }
                    }
                    else{
                        /* Not a proper UTF-16 char sequence */
                        return JAVACALL_INVALID_ARGUMENT;
                    }
                }
            }
        }

        if (pUtf16 != NULL) {
            if (len + count > utf16Len) {
                /* Can't continue, result buffer too small */
                return JAVACALL_FAIL;
            } else {
                int i;
                for (i = 0; i < count; i++) {
                    pUtf16[len+i] = output[i];
                }
            }
        }

        len += count;
    }

    if (length != NULL) {
       *length = len;
    }
    return JAVACALL_OK;
}

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
                                      javacall_const_utf16_string str2) {
    if (str1 == NULL || str2 == NULL) {
        return JAVACALL_FALSE;
    }

    do{
        if (*str1 != *str2) {
            return JAVACALL_FALSE;
        }
        str1++;
        str2++;
    }
    while (*str1 || *str2);

    return JAVACALL_TRUE;
}

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
                                     /*OUT*/ javacall_int32* comparison) {
    javacall_utf16 ch1;
    javacall_utf16 ch2;
    javacall_int32 ucs4_1;
    javacall_int32 ucs4_2;

    if (comparison == NULL || str1 == NULL || str2 == NULL) {
        return JAVACALL_FAIL;
    }

    *comparison = 0;
    do{
        ch1 = (javacall_utf16) *str1++;
        ch2 = (javacall_utf16) *str2++;

        ucs4_1 = 0xFFFFFFFF;
        ucs4_2 = 0xFFFFFFFF;

        if (ch1 >= 0xd800 && ch1 <= 0xdfff) {
            if (ch1 > 0xdbff) {
                return JAVACALL_INVALID_ARGUMENT;
            }

            {
                javacall_utf16 low_char = *str1++;
                if (low_char < 0xdc00 || low_char > 0xdfff) {
                    return JAVACALL_INVALID_ARGUMENT;
                }
                ucs4_1 = (ch1 - 0xd800) * 0x400 + (low_char - 0xdc00) + 0x10000;
            }
        }

        if (ch2 >= 0xd800 && ch2 <= 0xdfff) {
            if (ch2 > 0xdbff) {
                return JAVACALL_INVALID_ARGUMENT;
            }

            {
                javacall_utf16 low_char = *str2++;
                if (low_char < 0xdc00 || low_char > 0xdfff) {
                    return JAVACALL_INVALID_ARGUMENT;
                }
                ucs4_2 = (ch2 - 0xd800) * 0x400 + (low_char - 0xdc00) + 0x10000;
            }
        }

        if (ch1 != ch2) {
            if (ucs4_2 != 0xFFFFFFFF) { /* ch2 is not 16-unit symbol */
                if (ucs4_1 != 0xFFFFFFFF) { /* ch1 is not 16-unit symbol */
                    *comparison = ucs4_1 - ucs4_2;
                }
                else{/* ch1 is 16-unit symbol */
                    *comparison = (javacall_int32)ch1 - ucs4_2;
                }
            }
            else{
                if (ucs4_1 != 0xFFFFFFFF) { /* ch1 is not 16-unit symbol */
                    *comparison = ucs4_1 - (javacall_int32)ch2;
                }
                else{ /* ch1 is 16-unit symbol */
                    *comparison = (javacall_int32)ch1 - (javacall_int32)ch2;
                }
            }
            break;
        }
        else{
            if (ucs4_1 != ucs4_1) {
                *comparison = ucs4_1 - ucs4_2;
                break;
            }
        }
    }
    while (ch1 && ch2);

    return JAVACALL_OK;
}

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
                                      /*OUT*/ javacall_int32* comparison) {
    javacall_utf16 ch1;
    javacall_utf16 ch2;
    javacall_int32 ucs4_1;
    javacall_int32 ucs4_2;

    if (comparison == NULL || str1 == NULL || str2 == NULL) {
        return JAVACALL_FAIL;
    }

    *comparison = 0;
    do{
        if (number-- == 0) {
            return JAVACALL_OK;
        }

        ch1 = (javacall_utf16) *str1++;
        ch2 = (javacall_utf16) *str2++;

        ucs4_1 = 0xFFFFFFFF;
        ucs4_2 = 0xFFFFFFFF;

        if (ch1 >= 0xd800 && ch1 <= 0xdfff) {
            if (ch1 > 0xdbff) {
                return JAVACALL_INVALID_ARGUMENT;
            }

            {
                javacall_utf16 low_char = *str1++;
                if (low_char < 0xdc00 || low_char > 0xdfff) {
                    return JAVACALL_INVALID_ARGUMENT;
                }
                ucs4_1 = (ch1 - 0xd800) * 0x400 + (low_char - 0xdc00) + 0x10000;
            }
        }

        if (ch2 >= 0xd800 && ch2 <= 0xdfff) {
            if (ch2 > 0xdbff) {
                return JAVACALL_INVALID_ARGUMENT;
            }

            {
                javacall_utf16 low_char = *str2++;
                if (low_char < 0xdc00 || low_char > 0xdfff) {
                    return JAVACALL_INVALID_ARGUMENT;
                }
                ucs4_2 = (ch2 - 0xd800) * 0x400 + (low_char - 0xdc00) + 0x10000;
            }
        }

        if (ch1 != ch2) {
            if (ucs4_2 != 0xFFFFFFFF) { /* ch2 is not 16-unit symbol */
                if (ucs4_1 != 0xFFFFFFFF) { /* ch1 is not 16-unit symbol */
                    *comparison = ucs4_1 - ucs4_2;
                }
                else{/* ch1 is 16-unit symbol */
                    *comparison = (javacall_int32)ch1 - ucs4_2;
                }
            }
            else{
                if (ucs4_1 != 0xFFFFFFFF) { /* ch1 is not 16-unit symbol */
                    *comparison = ucs4_1 - (javacall_int32)ch2;
                }
                else{ /* ch1 is 16-unit symbol */
                    *comparison = (javacall_int32)ch1 - (javacall_int32)ch2;
                }
            }
            break;
        }
        else{
            if (ucs4_1 != ucs4_1) {
                *comparison = ucs4_1 - ucs4_2;
                break;
            }
        }
    }
    while (ch1 && ch2);

    return JAVACALL_OK;
}

#define ISALFA(c) ((((c) > 0x40) && ((c) < 0x5B)) || (((c) > 0x60) && ((c) < 0x7B)))
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
                                      /*OUT*/ javacall_int32* comparison) {
    javacall_utf16 ch1;
    javacall_utf16 ch2;
    javacall_int32 ucs4_1;
    javacall_int32 ucs4_2;

    if (comparison == NULL || str1 == NULL || str2 == NULL) {
        return JAVACALL_FAIL;
    }

    *comparison = 0;
    do{
        ch1 = (javacall_utf16) *str1++;
        ch2 = (javacall_utf16) *str2++;

        ucs4_1 = 0xFFFFFFFF;
        ucs4_2 = 0xFFFFFFFF;

        if (ch1 >= 0xd800 && ch1 <= 0xdfff) {
            if (ch1 > 0xdbff) {
                return JAVACALL_INVALID_ARGUMENT;
            }

            {
                javacall_utf16 low_char = *str1++;
                if (low_char < 0xdc00 || low_char > 0xdfff) {
                    return JAVACALL_INVALID_ARGUMENT;
                }
                ucs4_1 = (ch1 - 0xd800) * 0x400 + (low_char - 0xdc00) + 0x10000;
            }
        }

        if (ch2 >= 0xd800 && ch2 <= 0xdfff) {
            if (ch2 > 0xdbff) {
                return JAVACALL_INVALID_ARGUMENT;
            }

            {
                javacall_utf16 low_char = *str2++;
                if (low_char < 0xdc00 || low_char > 0xdfff) {
                    return JAVACALL_INVALID_ARGUMENT;
                }
                ucs4_2 = (ch2 - 0xd800) * 0x400 + (low_char - 0xdc00) + 0x10000;
            }
        }

        if (ch1 != ch2) {
            if (ucs4_2 != 0xFFFFFFFF) { /* ch2 is not 16-unit symbol */
                if (ucs4_1 != 0xFFFFFFFF) { /* ch1 is not 16-unit symbol */
                    *comparison = ucs4_1 - ucs4_2;
                }
                else{/* ch1 is 16-unit symbol */
                    *comparison = (javacall_int32)ch1 - ucs4_2;
                }
            }
            else{
                if (ucs4_1 != 0xFFFFFFFF) { /* ch1 is not 16-unit symbol */
                    *comparison = ucs4_1 - (javacall_int32)ch2;
                }
                else{ /* ch1 is 16-unit symbol */
                    if (((ch1 ^ ch2) != 0x20) || !ISALFA(ch1))  {
                        *comparison = (javacall_int32)ch1 - (javacall_int32)ch2;
                    }
                    else{
                        continue;
                    }
                }
            }
            break;
        }

        if (ucs4_1 != ucs4_1) {
            *comparison = ucs4_1 - ucs4_2;
            break;
        }
    }
    while (ch1 && ch2);

    return JAVACALL_OK;
}

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
                                       /*OUT*/ javacall_int32* comparison) {
    javacall_utf16 ch1;
    javacall_utf16 ch2;
    javacall_int32 ucs4_1;
    javacall_int32 ucs4_2;

    if (comparison == NULL || str1 == NULL || str2 == NULL) {
        return JAVACALL_FAIL;
    }

    *comparison = 0;
    do{
        if (number-- == 0) {
            return JAVACALL_OK;
        }

        ch1 = (javacall_utf16) *str1++;
        ch2 = (javacall_utf16) *str2++;

        ucs4_1 = 0xFFFFFFFF;
        ucs4_2 = 0xFFFFFFFF;

        if (ch1 >= 0xd800 && ch1 <= 0xdfff) {
            if (ch1 > 0xdbff) {
                return JAVACALL_INVALID_ARGUMENT;
            }

            {
                javacall_utf16 low_char = *str1++;
                if (low_char < 0xdc00 || low_char > 0xdfff) {
                    return JAVACALL_INVALID_ARGUMENT;
                }
                ucs4_1 = (ch1 - 0xd800) * 0x400 + (low_char - 0xdc00) + 0x10000;
            }
        }

        if (ch2 >= 0xd800 && ch2 <= 0xdfff) {
            if (ch2 > 0xdbff) {
                return JAVACALL_INVALID_ARGUMENT;
            }

            {
                javacall_utf16 low_char = *str2++;
                if (low_char < 0xdc00 || low_char > 0xdfff) {
                    return JAVACALL_INVALID_ARGUMENT;
                }
                ucs4_2 = (ch2 - 0xd800) * 0x400 + (low_char - 0xdc00) + 0x10000;
            }
        }

        if (ch1 != ch2) {
            if (ucs4_2 != 0xFFFFFFFF) { /* ch2 is not 16-unit symbol */
                if (ucs4_1 != 0xFFFFFFFF) { /* ch1 is not 16-unit symbol */
                    *comparison = ucs4_1 - ucs4_2;
                }
                else{/* ch1 is 16-unit symbol */
                    *comparison = (javacall_int32)ch1 - ucs4_2;
                }
            }
            else{
                if (ucs4_1 != 0xFFFFFFFF) { /* ch1 is not 16-unit symbol */
                    *comparison = ucs4_1 - (javacall_int32)ch2;
                }
                else{ /* ch1 is 16-unit symbol */
                    if (((ch1 ^ ch2) != 0x20) || !ISALFA(ch1))  {
                        *comparison = (javacall_int32)ch1 - (javacall_int32)ch2;
                    }
                    else{
                        continue;
                    }
                }
            }
            break;
        }
        else{
            if (ucs4_1 != ucs4_1) {
                *comparison = ucs4_1 - ucs4_2;
                break;
            }
        }
    }
    while (ch1 && ch2);

    return JAVACALL_OK;
}

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
                                     javacall_int32 dstLen) {
    javacall_int32 length1, length2, length;

    if (dst == NULL) {
        return JAVACALL_FAIL;
    }

    if (javautil_unicode_utf16_ulength(str1, &length1) != JAVACALL_OK) {
        length1 = 0;
    }

    if (javautil_unicode_utf16_ulength(str2, &length2) != JAVACALL_OK) {
        length2 = 0;
    }

    if (!length1 && !length2 ) { /* both strings are empty or NULL */
        *dst = 0;
        return JAVACALL_OK;
    }

    length = length1 + length2 + 1;
    if (dstLen < length) {
        return JAVACALL_OUT_OF_MEMORY;
    }

    if (length1 > 0) {
        memcpy(dst, str1, length1 * sizeof(javacall_utf16));
    }

    if (length2 > 0) {
        memcpy(dst + length1, str2, length2 * sizeof(javacall_utf16));
    }

    dst[length - 1] = 0;
    return JAVACALL_OK;
}

#define RADIX 10
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
#define INT_SIZE_IN_BITS 32
//#define MIN_INT (((javacall_int32)1) << (INT_SIZE_IN_BITS - 1))
#define MIN_INT 0x80000000   //min negative integer
javacall_result javautil_unicode_to_int32(javacall_const_utf16_string str,
                                          /*OUT*/ javacall_int32* number) {
    javacall_int32 length;
    javacall_int32 res;
    unsigned char isNegative = 0;
    javacall_int32 td = 1;
    javacall_int32 i;

    if (str == NULL || number == NULL) {
        return JAVACALL_FAIL;
    }
    javautil_unicode_utf16_ulength(str, &length);
    if (length == 0) {
        return JAVACALL_FAIL;
    }

    /* maybe first symbol is '-' or '+' */
    switch (str[0]) {
        case '-':
            isNegative = 1;
        case '+':
            str++;
            length--;
    }

    for (i = length-1, res = 0; i >= 0; i--) {
        if ((str[i] >= '0') && (str[i] <= '9')) { /* range between 0 to 9 */
            res += (str[i]-'0')*td;
            td*=RADIX;
            if (res < 0 && res != MIN_INT) {
                return JAVACALL_INVALID_ARGUMENT;
            }
        } else {
            return JAVACALL_INVALID_ARGUMENT;
        }
    }

    if (isNegative) {
        res = - res;
    }
    else{
        if (res == MIN_INT) {
            /* arithmetic overflow */
            return JAVACALL_INVALID_ARGUMENT;
        }
    }

    *number = res;
    return JAVACALL_OK;
}

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
#define DIGIT(x) ('0'+((unsigned char)(x)))
#define TEXT_BUF_FOR_INT_SIZE 11 /* Max length has MIN_INT = -2147483648 */
javacall_result javautil_unicode_from_int32(javacall_int32 number,
                                            /*OUT*/ javacall_utf16_string str,
                                            javacall_int32 strLen) {
    unsigned char isNegative = (number < 0);
    javacall_utf16 buf[TEXT_BUF_FOR_INT_SIZE+1] = {
        '-', '2', '1', '4', '7', '4', '8', '3', '6', '4', '8', 0
    };
    /* filling buffer from the end */
    javacall_int32 charPos = TEXT_BUF_FOR_INT_SIZE;
    javacall_int32 length;

    if (str == NULL) {
        return JAVACALL_FAIL;
    }

    if (number != MIN_INT) {
        /* Platform-independent division: dividend and divisor must be positive */
        if (isNegative) {
            number = -number;
        }

        /* Trailing 0 */
        buf[charPos--] = 0;
        while (number >= RADIX) {
            buf[charPos--] = (javacall_utf16) DIGIT(number % RADIX);
            number /= RADIX;
        }
        buf[charPos] = (javacall_utf16) DIGIT(number);

        if (isNegative) {
             buf[--charPos] = '-';
        }
    }
    else{
        charPos = 0;
    }

    length = (TEXT_BUF_FOR_INT_SIZE - charPos + 1);
    if (strLen < length) {
        return JAVACALL_OUT_OF_MEMORY;
    }

    /* Filling the buffer */
    memcpy(str, &buf[charPos], length * sizeof(javacall_utf16));
    return JAVACALL_OK;
}

/**
 * Remove white spaces from the end of a string
 *
 * @param str string to trim
 * @return <code>JAVACALL_OK</code> on success
 *         <code>JAVACALL_FAIL</code>if str is NULL or has zero length
 */
javacall_result javautil_unicode_trim(javacall_utf16_string str) {
    javacall_int32 length;

    if (str == NULL) {
        return JAVACALL_FAIL;
    }

    javautil_unicode_utf16_ulength(str, &length);
    if (length == 0) {
        return JAVACALL_FAIL;
    }

    while (((*(str+length-1) == SPACE) ||
            (*(str+length-1) == HTAB)) && (length > 0)) {
        *(str+length-1) = 0;
        length--;
    }

    return JAVACALL_OK;
}

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
                                           javacall_int32 destLen) {
    javacall_int32 startUnit = 0;
    javacall_int32 temp;
    javacall_int32 dstLen;
    javacall_const_utf16_string ptr;
    javacall_utf16 input_char = 0;

    if ((src == NULL) || (dest == NULL)) {
        return JAVACALL_FAIL;
    }

    if ((begin < 0) || (begin >= end)) {
        return JAVACALL_INVALID_ARGUMENT;
    }

    for(ptr = src, temp = 0; *ptr != 0; temp++) {
        input_char = *ptr++;
        if (input_char >= 0xd800 && input_char <= 0xdfff) {
            if (input_char > 0xdbff) {
                return JAVACALL_INVALID_ARGUMENT;
            }

            /* this is <high-half zone code> in UTF-16 */
            /* check next char is valid <low-half zone code> */
            {
                javacall_utf16 low_char = *ptr++;
                if (low_char < 0xdc00 || low_char > 0xdfff) {
                    return JAVACALL_INVALID_ARGUMENT;
                }

                if (temp == begin) {
                    startUnit = (javacall_int32)(ptr - src);
                }

                if (temp == end) {
                    ptr--;
                    break;
                }
            }
        }
        else{
            if (temp == begin){
                startUnit = (javacall_int32)(ptr - src - 1);
            }

            if (temp == end){
                break;
            }
        }
    }

    if (temp != end){
        return JAVACALL_INVALID_ARGUMENT;
    }

    dstLen = (javacall_int32)(ptr - src - startUnit);
    if (destLen < dstLen) {
        return JAVACALL_OUT_OF_MEMORY;
    }

    memcpy(dest, src + startUnit, dstLen * sizeof(javacall_utf16));
    dest[dstLen - 1] = 0;
    return JAVACALL_OK;
}

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
                                          /* OUT */ javacall_int32* index) {
    javacall_utf16 code_unit[2];
    javacall_int32 unit_length;
    javacall_utf16 input_char = 0;
    javacall_int32 len;

    if (str == NULL || index == NULL) {
        return JAVACALL_FAIL;
    }

    if (javautil_unicode_codepoint_to_utf16codeunit(ch, code_unit, &unit_length) !=
        JAVACALL_OK) {
        return JAVACALL_INVALID_ARGUMENT;
    }

    for(len = 0; *str != 0; len++) {
        input_char = *str++;
        if (input_char >= 0xd800 && input_char <= 0xdfff) {
            if (input_char > 0xdbff) {
                return JAVACALL_INVALID_ARGUMENT;
            }

            /* this is <high-half zone code> in UTF-16 */
            /* check next char is valid <low-half zone code> */
            {
                javacall_utf16 low_char = *str++;
                if (low_char < 0xdc00 || low_char > 0xdfff) {
                    return JAVACALL_INVALID_ARGUMENT;
                }

                if ((unit_length == 2) &&
                    (input_char == code_unit[0]) &&
                    (low_char == code_unit[1]) ) {
                    break;
                }
            }
        }
        else {
            if ((unit_length == 1) && (input_char == code_unit[0])) {
                break;
            }
        }
    }

    if(*str == 0) {
        return JAVACALL_FAIL;
    }

    *index = len;
    return JAVACALL_OK;
}

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
                                               /* OUT */ javacall_int32* index) {
    javacall_utf16 code_unit[2];
    javacall_int32 unit_length;
    javacall_utf16 input_char = 0;
    javacall_int32 len;

    if (str == NULL || index == NULL) {
        return JAVACALL_FAIL;
    }

    if (javautil_unicode_codepoint_to_utf16codeunit(ch, code_unit, &unit_length) !=
        JAVACALL_OK) {
        return JAVACALL_INVALID_ARGUMENT;
    }

    *index = 0xFFFFFFFF;
    for(len = 0; *str != 0; len++) {
        input_char = *str++;
        if (input_char >= 0xd800 && input_char <= 0xdfff) {
            if (input_char > 0xdbff) {
                return JAVACALL_INVALID_ARGUMENT;
            }

            /* this is <high-half zone code> in UTF-16 */
            /* check next char is valid <low-half zone code> */
            {
                javacall_utf16 low_char = *str++;
                if (low_char < 0xdc00 || low_char > 0xdfff) {
                    return JAVACALL_INVALID_ARGUMENT;
                }

                if ((unit_length == 2) &&
                    (input_char == code_unit[0]) &&
                    (low_char == code_unit[1]) ) {
                    *index = len;
                }
            }
        }
        else {
            if ((unit_length == 1) && (input_char == code_unit[0])) {
                *index = len;
            }
        }
    }

    if(*index == 0xFFFFFFFF) {
        return JAVACALL_FAIL;
    }

    return JAVACALL_OK;
}
