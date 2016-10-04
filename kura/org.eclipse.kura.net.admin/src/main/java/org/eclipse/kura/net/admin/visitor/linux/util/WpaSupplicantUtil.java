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
package org.eclipse.kura.net.admin.visitor.linux.util;

public class WpaSupplicantUtil {

    public static final int MODE_INFRA = 0;
    public static final int MODE_IBSS = 1;
    public static final int MODE_AP = 2;

    public static int convChannelToFrequency(int channel) {
        int frequency = -1;
        if (channel >= 1 && channel <= 13) {
            frequency = 2407 + channel * 5;
        }
        return frequency;
    }

    public static int convFrequencyToChannel(int mhz) {
        int channel = (mhz - 2407) / 5;
        return channel;
    }

}
