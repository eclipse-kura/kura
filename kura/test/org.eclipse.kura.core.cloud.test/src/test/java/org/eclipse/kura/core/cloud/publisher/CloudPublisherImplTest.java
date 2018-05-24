/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.core.cloud.publisher;

import org.eclipse.kura.KuraException;
import org.junit.Test;

public class CloudPublisherImplTest {

    @Test(expected = KuraException.class)
    public void testPublishNoCloudService() throws KuraException {
        CloudPublisherImpl cloudPublisherImpl = new CloudPublisherImpl();
        cloudPublisherImpl.publish(null);
    }

}
