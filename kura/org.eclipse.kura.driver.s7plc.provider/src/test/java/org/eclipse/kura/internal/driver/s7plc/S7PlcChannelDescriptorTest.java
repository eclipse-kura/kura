/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.internal.driver.s7plc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.eclipse.kura.core.configuration.metatype.Tad;
import org.junit.Test;


public class S7PlcChannelDescriptorTest {

    @Test
    public void testGetDescriptor() {
        S7PlcChannelDescriptor descriptor = new S7PlcChannelDescriptor();

        List<Tad> result = (List<Tad>) descriptor.getDescriptor();

        assertNotNull(result);
        assertEquals(5, result.size());

        assertEquals("s7.data.type", result.get(0).getName());
        assertEquals(S7PlcDataType.values().length, result.get(0).getOption().size());

        assertEquals("data.block.no", result.get(1).getName());
        assertEquals("offset", result.get(2).getName());
        assertEquals("byte.count", result.get(3).getName());
        assertEquals("bit.index", result.get(4).getName());
    }

}
