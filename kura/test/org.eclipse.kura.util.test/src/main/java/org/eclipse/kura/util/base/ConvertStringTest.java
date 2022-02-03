/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.util.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ConvertStringTest {

    private String string;

    @Test
    public void convertUTF8StringToHex() {
        givenString("kura€&");

        thenIsConvertedIn("6B757261E282AC26");
    }

    @Test
    public void unescapeUTF8String() {
        givenString("kura\\xe2\\x82\\xac&");

        thenIsUnescapedIn("kura€&");
    }

    private void thenIsUnescapedIn(String string) {
        assertEquals(string, StringUtil.unescapeUTF8String(this.string));
    }

    private void thenIsConvertedIn(String hex) {
        assertTrue(StringUtil.toHex(string).equalsIgnoreCase(hex));
    }

    private void givenString(String string) {
        this.string = string;
    }
}
