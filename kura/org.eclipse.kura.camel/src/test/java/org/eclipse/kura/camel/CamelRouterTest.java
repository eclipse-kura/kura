/*******************************************************************************
 * Copyright (c) 2011, 2016 Red Hat and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat
 *******************************************************************************/

package org.eclipse.kura.camel;

import org.eclipse.kura.camel.component.CamelRouter;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import static org.mockito.Mockito.*;

public class CamelRouterTest {

    CamelRouter camelRouter = new CamelRouter(){};

    @Test
    public void shouldReadBundleId() throws Exception {
        BundleContext bundleContext = mock(BundleContext.class, RETURNS_DEEP_STUBS);
        camelRouter.start(bundleContext);
        verify(bundleContext.getBundle(), atLeastOnce()).getBundleId();
    }

}