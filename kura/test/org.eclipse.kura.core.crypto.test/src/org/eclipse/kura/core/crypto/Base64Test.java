/*******************************************************************************
 * Copyright (c) 2016, 2020 Red Hat Inc and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.core.crypto;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class Base64Test {
    private CryptoServiceImpl cryptoService;

    @Before
    public void setup() {
        this.cryptoService = new CryptoServiceImpl();
    }

    @Test
    public void testEncode1() throws Exception {
        final String result = this.cryptoService.encodeBase64("foo-bar");
        assertEquals("Zm9vLWJhcg==", result);
    }

    @Test
    public void testDecode1() throws Exception {
        final String result = this.cryptoService.decodeBase64("Zm9vLWJhcg==");
        assertEquals("foo-bar", result);
    }
}
