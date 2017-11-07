/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.status;

import org.eclipse.kura.status.CloudConnectionStatusComponent;
import org.eclipse.kura.status.CloudConnectionStatusEnum;
import org.eclipse.kura.status.CloudConnectionStatusService;

public class IdleStatusComponent implements CloudConnectionStatusComponent {

    @Override
    public int getNotificationPriority() {
        return CloudConnectionStatusService.PRIORITY_MIN;
    }

    @Override
    public CloudConnectionStatusEnum getNotificationStatus() {
        return CloudConnectionStatusEnum.OFF;
    }

    @Override
    public void setNotificationStatus(CloudConnectionStatusEnum status) {
        // We need a always present minimum priority status of OFF, se we don't want
        // the default notification status to be changed.

        // Do nothing
    }

}
