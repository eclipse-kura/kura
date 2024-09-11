/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
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
