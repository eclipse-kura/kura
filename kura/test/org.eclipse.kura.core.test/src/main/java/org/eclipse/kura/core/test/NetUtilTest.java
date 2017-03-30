/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.test;

import org.eclipse.kura.core.util.NetUtil;
import org.eclipse.kura.test.annotation.TestTarget;
import org.junit.Test;

import junit.framework.TestCase;

public class NetUtilTest extends TestCase {

    @TestTarget(targetPlatforms = { TestTarget.PLATFORM_ALL })
    @Test
    public void testGetPrimaryMacAddress() {
        assertNotNull(NetUtil.getPrimaryMacAddress());
        assertFalse("UNKNOWN".equals(NetUtil.getPrimaryMacAddress()));
    }
}
