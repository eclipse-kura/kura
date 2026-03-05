/*******************************************************************************
 * Copyright (c) 2025, 2026 Eurotech and/or its affiliates and others
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
public class LibGPIOPinTest {

    @Parameters
    public static Collection<Object[]> ChipNameParams() {
        List<Object[]> params = new ArrayList<>();
        params.add(new Object[] { "/dev/gpiochip0", 0 });
        params.add(new Object[] { "/dev/gpiochip1", 1 });
        params.add(new Object[] { "/dev/gpiochip123", 123 });
        params.add(new Object[] { "", 0 });
        params.add(new Object[] { "without_numbers", 0 });
        params.add(new Object[] { "with_symbols!%£$", 0 });
        return params;
    }

    private final String inputChipName;
    private final int expectedChipNumber;
    private int calculatedChipNumber;

    public LibGPIOPinTest(String chipName, int chipNumber) {
        this.inputChipName = chipName;
        this.expectedChipNumber = chipNumber;
    }

    @Test
    public void testExtractChipNumber() {
        whenChipNumberIsExtracted();

        thenTheCorrectChipNumberIsReturned();
    }

    /*
     * When
     */

    public void whenChipNumberIsExtracted() {
        this.calculatedChipNumber = LibGpiodPin.extractChipNumber(this.inputChipName);
    }

    /*
     * Then
     */

    public void thenTheCorrectChipNumberIsReturned() {
        assertEquals(this.expectedChipNumber, this.calculatedChipNumber);
    }
}
