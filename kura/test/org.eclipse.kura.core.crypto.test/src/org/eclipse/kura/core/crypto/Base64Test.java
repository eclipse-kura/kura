/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc
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
