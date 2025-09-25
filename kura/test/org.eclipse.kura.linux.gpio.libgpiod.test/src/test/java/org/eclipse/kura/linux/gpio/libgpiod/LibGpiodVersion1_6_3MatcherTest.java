/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.linux.gpio.libgpiod;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class LibGpiodVersion1_6_3MatcherTest {

    @Parameters
    public static Collection<Object[]> versions() {
        List<Object[]> params = new ArrayList<>();
        params.add(new Object[] { "1.6.3", true });
        params.add(new Object[] { "1.6.0", true });
        params.add(new Object[] { "1.6.5", true });
        params.add(new Object[] { "1.7.0", false });
        params.add(new Object[] { "2.0.0", false });
        params.add(new Object[] { "2.2.3", false });
        params.add(new Object[] { "not.a.version", false });
        params.add(new Object[] { "", false });
        params.add(new Object[] { null, false });
        params.add(new Object[] { "abcdefg", false });
        return params;
    }

    private final String inputVersion;
    private final boolean expectedMatch;
    private boolean calculatedMatch;

    public LibGpiodVersion1_6_3MatcherTest(String version, boolean match) {
        this.inputVersion = version;
        this.expectedMatch = match;
    }

    @Test
    public void shouldMatchCorrectly() {
        whenCheckVersion();

        thenMatchIsCorrect();
    }

    private void whenCheckVersion() {
        this.calculatedMatch = LibGpiodVersionDetector.isVersion1_6_X(this.inputVersion);
    }

    private void thenMatchIsCorrect() {
        assertEquals(this.expectedMatch, this.calculatedMatch);
    }
}
