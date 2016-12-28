/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.kura.camel.type;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;

import org.eclipse.kura.camel.component.AbstractRouterTest;
import org.eclipse.kura.message.KuraPayload;
import org.junit.Test;

public class TypeConverterTest extends AbstractRouterTest {

    @Test
    public void testFromMap() {
        final KuraPayload result = getCamelContext().getTypeConverter().convertTo(KuraPayload.class,
                Collections.singletonMap("foo", "bar"));
        
        assertNotNull(result);
        assertNotNull(result.getTimestamp());

        assertEquals("bar", result.getMetric("foo"));
    }
}
